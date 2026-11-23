package ui.chat

import data.InMemorySettingsRepository
import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import domain.provider.MentorProvider
import domain.provider.ProviderFailure
import domain.usecase.SendMessageUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import platform.connectivity.ConnectivityObserver
import test.MainDispatcherRule
import test.fakes.ControlledMentorProvider
import test.fakes.FakeConversationRepository
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityRecoverySafetyTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private class FakeNetwork(initial: Boolean) : ConnectivityObserver {
        private val flow = MutableStateFlow(initial)
        override val isOnline: StateFlow<Boolean> get() = flow
        override fun refresh(): Boolean = flow.value
        fun set(online: Boolean) { flow.value = online }
    }

    @Test
    fun offlineRetryDoesNotDuplicatePersistedUserTurn() = runTest {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val settings = InMemorySettingsRepository().also { it.saveApiKey("test-only") }
        val provider = ControlledMentorProvider()
        val network = FakeNetwork(true)
        var nextMessageId = 0
        val vm = ActiveChatViewModel(
            conversationId = conversation.id,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
            connectivityObserver = network,
            newMessageId = {
              MessageId("message-${++nextMessageId}")
            },
        )
        runCurrent()

        vm.onAction(ActiveChatAction.DraftChanged("Explain connectivity"))
        vm.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        assertEquals(1, provider.requests.size)
        provider.fail(0, ProviderFailure.Transport(IOException("simulated")))
        runCurrent()
        assertEquals(ChatErrorKind.TRANSPORT, (vm.uiState.value.requestState as ChatRequestState.Error).kind)

        network.set(false)
        runCurrent()
        vm.onAction(ActiveChatAction.RetryClicked)
        runCurrent()
        assertEquals(1, provider.requests.size)
        assertEquals(listOf(Role.USER), repository.find(conversation.id)!!.messages.map { it.role })

        network.set(true)
        runCurrent()
        vm.onAction(ActiveChatAction.RetryClicked)
        runCurrent()
        assertEquals(2, provider.requests.size)
        assertEquals(provider.requests[0].history, provider.requests[1].history)
        provider.succeed(1, "Recovered")
        runCurrent()
        assertEquals(listOf(Role.USER, Role.MODEL), repository.find(conversation.id)!!.messages.map { it.role })
    }

    @Test
    fun confirmingRewriteWhileOfflineDoesNotModifyPersistedHistory() = runTest {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val original = conversation
            .append(Message(MessageId("u1"), Role.USER, "Original question", 0L))
            .append(Message(MessageId("m1"), Role.MODEL, "Valid answer", 1L))
        repository.save(original)
        val settings = InMemorySettingsRepository().also { it.saveApiKey("test-only") }
        val provider = object : MentorProvider {
            var requests = 0
            override suspend fun generateReply(history: List<Message>, apiKey: String): String {
                requests++
                return "Unexpected replacement"
            }
        }
        val network = FakeNetwork(false)
        val vm = ActiveChatViewModel(
            conversationId = conversation.id,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
            connectivityObserver = network,
        )
        runCurrent()

        vm.onAction(ActiveChatAction.EditMessageClicked("u1"))
        vm.onAction(ActiveChatAction.EditDraftChanged("Different question"))
        vm.onAction(ActiveChatAction.SaveEditClicked)
        runCurrent()
        assertTrue(vm.uiState.value.editState is MessageEditState.ConfirmRewrite)

        vm.onAction(ActiveChatAction.ConfirmRewriteClicked)
        runCurrent()
        assertEquals(original, repository.find(conversation.id))
        assertEquals(0, provider.requests)
        assertTrue(vm.uiState.value.editState is MessageEditState.ConfirmRewrite)
        assertFalse(vm.uiState.value.isOnline)
    }
}
