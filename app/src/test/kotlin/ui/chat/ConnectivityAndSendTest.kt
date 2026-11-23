package ui.chat

import data.InMemorySettingsRepository
import domain.model.Message
import domain.model.identity.MessageId
import domain.provider.MentorProvider
import domain.repository.ConversationRepository
import domain.usecase.SendMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.collect
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
import test.fakes.FakeConversationRepository
import ui.chat.model.ChatRequestState

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityAndSendTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private class FakeNetwork(initial: Boolean) : ConnectivityObserver {
        private val online = MutableStateFlow(initial)
        override val isOnline: StateFlow<Boolean> = online
        override fun refresh(): Boolean = online.value
        fun change(to: Boolean) { online.value = to }
    }

    private class CountingProvider : MentorProvider {
        var calls: Int = 0
        override suspend fun generateReply(history: List<Message>, apiKey: String): String {
            calls++
            return "Assistant reply"
        }
    }

    private suspend fun fixture(network: FakeNetwork): Triple<ActiveChatViewModel, FakeConversationRepository, CountingProvider> {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val settings = InMemorySettingsRepository().also { it.saveApiKey("test-key") }
        val provider = CountingProvider()
        var nextMessageId = 0
        val vm = ActiveChatViewModel(
            conversationId = conversation.id,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
            connectivityObserver = network,
            newMessageId = { MessageId("message-${++nextMessageId}") },
        )
        return Triple(vm, repository, provider)
    }

    @Test
    fun `offline prohibits request and preserves unsent draft`() = runTest {
        val network = FakeNetwork(false)
        val (vm, repository, provider) = fixture(network)
        runCurrent()
        vm.onAction(ActiveChatAction.DraftChanged("Hello"))
        vm.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        assertFalse(vm.uiState.value.canSend)
        assertEquals("Hello", vm.uiState.value.draft)
        assertTrue(repository.find(vm.conversationId)!!.messages.isEmpty())
        assertEquals(0, provider.calls)
    }

    @Test
    fun `reconnecting allows send once and persists reply`() = runTest {
        val network = FakeNetwork(false)
        val (vm, repository, provider) = fixture(network)
        runCurrent()
        vm.onAction(ActiveChatAction.DraftChanged("Hello"))
        network.change(true)
        runCurrent()
        assertTrue(vm.uiState.value.canSend)
        vm.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        assertEquals(1, provider.calls)
        assertEquals(2, repository.find(vm.conversationId)!!.messages.size)
        assertEquals(ChatRequestState.Idle, vm.uiState.value.requestState)
    }

    @Test
    fun `network loss restoration and manual check emit accurate notices`() = runTest {
        val network = FakeNetwork(true)
        val (vm, _, _) = fixture(network)
        runCurrent()
        val events = mutableListOf<ui.chat.model.ConnectivityNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.connectivityNotices.collect { events += it }
        }
        network.change(false)
        runCurrent()
        assertFalse(vm.uiState.value.isOnline)
        assertEquals(false, events.last().online)
        assertFalse(events.last().manualCheck)

        vm.onAction(ActiveChatAction.CheckConnectivityClicked)
        assertFalse(events.last().online)
        assertTrue(events.last().manualCheck)

        network.change(true)
        runCurrent()
        assertTrue(vm.uiState.value.isOnline)
        assertEquals(true, events.last().online)
        assertFalse(events.last().manualCheck)
    }

    @Test
    fun `offline check emits confirmation and keeps network disabled`() = runTest {
        val network = FakeNetwork(false)
        val (vm, _, _) = fixture(network)
        runCurrent()
        vm.onAction(ActiveChatAction.CheckConnectivityClicked)
        runCurrent()
        assertFalse(vm.uiState.value.isOnline)
    }
}
