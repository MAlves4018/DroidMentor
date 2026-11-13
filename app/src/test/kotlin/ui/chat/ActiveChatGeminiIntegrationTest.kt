package ui.chat

import data.InMemorySettingsRepository
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import domain.provider.ProviderFailure
import domain.usecase.SendMessageUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import test.MainDispatcherRule
import test.fakes.ControlledMentorProvider
import test.fakes.FakeConversationRepository
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveChatGeminiIntegrationTest {

    @get:Rule val dispatcherRule = MainDispatcherRule()

    private class Fixture(
        val repository: FakeConversationRepository,
        val conversationId: ConversationId,
    ) {
        val settings = InMemorySettingsRepository()
        val provider = ControlledMentorProvider()
        private var nextId = 0

        val viewModel = ActiveChatViewModel(
            conversationId = conversationId,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
            newMessageId = { MessageId("message-${++nextId}") },
        )

        val messages get() = repository.find(conversationId)!!.messages

        fun typeAndSend(text: String) {
            viewModel.onAction(ActiveChatAction.DraftChanged(text))
            viewModel.onAction(ActiveChatAction.SendClicked)
        }
    }

    private suspend fun fixture(): Fixture {
        val repository = FakeConversationRepository()
        return Fixture(repository, repository.create().id)
    }

    @Test
    fun successfulSendCommitsOneUserThenOneModelAndRendersIt() = runTest {
        val f = fixture()
        f.settings.saveApiKey("dummy-key")
        runCurrent()
        f.typeAndSend("  What is StateFlow?  ")
        assertEquals(listOf(Role.USER), f.messages.map { it.role })
        assertEquals("What is StateFlow?", f.messages.single().content)
        assertEquals(
            ChatRequestState.Generating,
            f.viewModel.uiState.value.requestState
        )
        assertFalse(f.viewModel.uiState.value.canSend)
        runCurrent()
        assertEquals(1, f.provider.requests.size)
        assertEquals(f.messages, f.provider.requests.single().history)
        assertEquals("dummy-key", f.provider.requests.single().apiKey)
        f.provider.succeed(0, "  StateFlow holds state.  ")
        runCurrent()
        assertEquals(listOf(Role.USER, Role.MODEL), f.messages.map { it.role })
        assertEquals(
            "StateFlow holds state.",
            f.viewModel.uiState.value.messages.last().content
        )
        assertEquals(
            ChatRequestState.Idle,
            f.viewModel.uiState.value.requestState
        )
        assertEquals(2, f.repository.savedIds.size)
    }

    @Test
    fun duplicateSubmissionsWhilePendingDoNotCreateMessagesOrExtraRequests() = runTest {
        val f = fixture()
        f.settings.saveApiKey("dummy-key")
        runCurrent()
        f.typeAndSend("One")
        f.viewModel.onAction(ActiveChatAction.DraftChanged("Two"))
        f.viewModel.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        assertEquals(1, f.messages.size)
        assertEquals(1, f.provider.requests.size)
        f.provider.succeed(0, "Answer")
        runCurrent()
        assertEquals(2, f.messages.size)
    }

    @Test
    fun failureKeepsUserWithoutModelAndRetryDoesNotAppendUserAgain() = runTest {
        val f = fixture()
        f.settings.saveApiKey("dummy-key")
        runCurrent()
        f.typeAndSend("Retry this")
        runCurrent()
        f.provider.fail(0, ProviderFailure.RateLimited("30"))
        runCurrent()
        assertEquals(1, f.messages.size)
        assertEquals(
            ChatRequestState.Error(ChatErrorKind.RATE_LIMITED),
            f.viewModel.uiState.value.requestState
        )
        f.viewModel.onAction(ActiveChatAction.RetryClicked)
        f.viewModel.onAction(ActiveChatAction.RetryClicked)
        runCurrent()
        assertEquals(1, f.messages.size)
        assertEquals(2, f.provider.requests.size)
        assertEquals(
            f.provider.requests[0].history,
            f.provider.requests[1].history
        )
        f.provider.succeed(1, "Recovered")
        runCurrent()
        assertEquals(
            listOf(Role.USER, Role.MODEL),
            f.messages.map { it.role }
        )
    }

    @Test
    fun stopCancelsAndLateCompletionDoesNotPublishModel() = runTest {
        val f = fixture()
        f.settings.saveApiKey("dummy-key")
        runCurrent()
        f.typeAndSend("Cancel")
        runCurrent()
        f.viewModel.onAction(ActiveChatAction.StopClicked)
        f.provider.requests.single().result.complete("Too late")
        runCurrent()
        assertEquals(
            ChatRequestState.Stopped,
            f.viewModel.uiState.value.requestState
        )
        assertEquals(listOf(Role.USER), f.messages.map { it.role })
    }

    @Test
    fun missingKeyBlocksSendAndNeverContactsProvider() = runTest {
        val f = fixture()
        runCurrent()
        f.typeAndSend("No key")
        runCurrent()
        assertFalse(f.viewModel.uiState.value.providerConfigured)
        assertEquals(0, f.messages.size)
        assertTrue(f.provider.requests.isEmpty())
    }

    @Test
    fun configuredStateFollowsSettingsChanges() = runTest {
        val f = fixture()
        runCurrent()
        assertFalse(f.viewModel.uiState.value.providerConfigured)
        f.settings.saveApiKey("key")
        runCurrent()
        assertTrue(f.viewModel.uiState.value.providerConfigured)
        f.settings.removeApiKey()
        runCurrent()
        assertFalse(f.viewModel.uiState.value.providerConfigured)
    }

    @Test
    fun mappedFailuresNeverInventAssistantTurns() = runTest {
        val cases = listOf(
            ProviderFailure.Offline to ChatErrorKind.OFFLINE,
            ProviderFailure.Authentication to ChatErrorKind.INVALID_API_KEY,
            ProviderFailure.ServerError(503) to ChatErrorKind.SERVER_UNAVAILABLE,
            ProviderFailure.Timeout to ChatErrorKind.TIMEOUT,
            ProviderFailure.Transport(IOException("network down")) to ChatErrorKind.TRANSPORT,
            ProviderFailure.Protocol() to ChatErrorKind.PROTOCOL,
            ProviderFailure.HttpError(400) to ChatErrorKind.GENERIC_PROVIDER_ERROR,
        )

        for ((failure, kind) in cases) {
            val f = fixture()
            f.settings.saveApiKey("dummy-key")
            runCurrent()
            f.typeAndSend("Explain")
            runCurrent()
            f.provider.fail(0, failure)
            runCurrent()
            assertEquals(
                ChatRequestState.Error(kind),
                f.viewModel.uiState.value.requestState
            )
            assertEquals(listOf(Role.USER), f.messages.map { it.role })
        }
    }

    @Test
    fun deletingConversationDuringRequestCannotResurrectIt() = runTest {
        val f = fixture()
        f.settings.saveApiKey("dummy-key")
        runCurrent()
        f.typeAndSend("Will be deleted")
        runCurrent()
        f.repository.delete(f.conversationId)
        f.provider.succeed(0, "Orphan")
        runCurrent()
        assertNull(f.repository.find(f.conversationId))
    }

    @Test
    fun responseStaysInOriginalConversationAfterAnotherChatOpens() = runTest {
        val repository = FakeConversationRepository()
        val first = repository.create().id
        val second = repository.create().id
        val settings = InMemorySettingsRepository()
        settings.saveApiKey("dummy")
        val provider = ControlledMentorProvider()

        val firstVm = ActiveChatViewModel(
            conversationId = first,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
        )

        runCurrent()
        firstVm.onAction(ActiveChatAction.DraftChanged("In first"))
        firstVm.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        repository.activate(second)
        provider.succeed(0, "Reply to first")
        runCurrent()
        assertEquals(2, repository.find(first)!!.messages.size)
        assertEquals(0, repository.find(second)!!.messages.size)
        assertEquals(second, repository.activeId)
    }
}
