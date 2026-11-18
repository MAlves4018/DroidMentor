package ui.chat

import data.InMemorySettingsRepository
import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import domain.provider.ProviderFailure
import domain.usecase.SendMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import test.MainDispatcherRule
import test.fakes.ControlledMentorProvider
import test.fakes.FakeConversationRepository
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState

/**
 * Candidate regression tests for DB-003. The first test intentionally exposes a
 * known gap in PR #68: a stale provider response can leave the UI Generating.
 * This file must NOT be reported as green until the production fix is applied.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DB003RequestConsistencyRegressionTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun staleProviderReplyDoesNotPersistAndDoesNotLeaveGenerating() = runTest {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val settings = InMemorySettingsRepository()
        settings.saveApiKey("test-only-key")
        val provider = ControlledMentorProvider()
        var nextId = 0
        val vm = ActiveChatViewModel(
            conversationId = conversation.id,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
            newMessageId = { MessageId("request-message-${++nextId}") },
        )
        runCurrent()

        vm.onAction(ActiveChatAction.DraftChanged("Initial question"))
        vm.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        assertEquals(1, provider.requests.size)
        assertEquals(ChatRequestState.Generating, vm.uiState.value.requestState)

        // Simulate a permitted repository mutation by another holder while
        // Gemini is answering. The request's captured history is now stale.
        val before = requireNotNull(repository.find(conversation.id))
        repository.save(before.append(
            Message(
                id = MessageId("external-user"),
                role = Role.USER,
                content = "Newer persisted question",
                sequence = before.nextSequence,
            )
        ))

        provider.succeed(0, "OBSOLETE RESPONSE")
        runCurrent()

        val persisted = requireNotNull(repository.find(conversation.id))
        assertEquals(
            listOf("Initial question", "Newer persisted question"),
            persisted.messages.map { it.content }
        )
        assertFalse(persisted.messages.any { it.content == "OBSOLETE RESPONSE" })

        // This assertion fails on the reviewed commit ae2bcadf: after the
        // stale-history return@launch the completed job leaves Generating set.
        assertFalse(
            "Completed stale request must leave a terminal UI state",
            vm.uiState.value.requestState is ChatRequestState.Generating
        )

        // Recovery must use the committed history, not repeatedly retry the
        // obsolete holder snapshot or overwrite the externally appended USER.
        vm.onAction(ActiveChatAction.RetryClicked)
        runCurrent()
        assertEquals("Retry must reach the provider with the current history", 2, provider.requests.size)
        assertEquals(persisted.messages, provider.requests[1].history)
        provider.succeed(1, "Current response")
        runCurrent()
        assertEquals(ChatRequestState.Idle, vm.uiState.value.requestState)
        assertEquals(
            listOf("Initial question", "Newer persisted question", "Current response"),
            repository.find(conversation.id)?.messages?.map { it.content }
        )
    }

    @Test
    fun unchangedEditIsNoOpAndKeepsValidSuffix() = runTest {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val existing = conversation
            .append(Message(MessageId("u1"), Role.USER, "Original", 0L))
            .append(Message(MessageId("a1"), Role.MODEL, "Existing answer", 1L))
        repository.save(existing)

        val settings = InMemorySettingsRepository()
        settings.saveApiKey("test-only-key")
        val provider = ControlledMentorProvider()
        val vm = ActiveChatViewModel(
            conversationId = conversation.id,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
        )
        runCurrent()

        vm.onAction(ActiveChatAction.EditMessageClicked("u1"))
        vm.onAction(ActiveChatAction.EditDraftChanged("Original"))
        vm.onAction(ActiveChatAction.SaveEditClicked)
        runCurrent()

        assertEquals(MessageEditState.None, vm.uiState.value.editState)
        assertEquals(existing, repository.find(conversation.id))
        assertTrue(provider.requests.isEmpty())
    }

    @Test
    fun deletedConversationBeforeHistoryReloadDoesNotLeaveGenerating() = runTest {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val settings = InMemorySettingsRepository()
        settings.saveApiKey("test-only-key")
        val provider = ControlledMentorProvider()
        val vm = ActiveChatViewModel(
            conversationId = conversation.id,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
            newMessageId = { MessageId("pending-user") },
        )
        runCurrent()

        // Send persists the USER message, then queues requestReply's coroutine.
        vm.onAction(ActiveChatAction.DraftChanged("Before deletion"))
        vm.onAction(ActiveChatAction.SendClicked)
        assertEquals(ChatRequestState.Generating, vm.uiState.value.requestState)
        repository.delete(conversation.id)
        runCurrent()

        assertTrue(repository.find(conversation.id) == null)
        assertTrue(provider.requests.isEmpty())
        assertFalse("A finished request must not retain Generating", vm.uiState.value.isGenerating)
    }

    @Test
    fun failureAfterConversationDeletionDoesNotLeaveGeneratingOrResurrectIt() = runTest {
        for (failure in listOf(ProviderFailure.Offline, IllegalStateException("test storage failure"))) {
            val repository = FakeConversationRepository()
            val conversation = repository.create()
            val settings = InMemorySettingsRepository()
            settings.saveApiKey("test-only-key")
            val provider = ControlledMentorProvider()
            val vm = ActiveChatViewModel(
                conversation.id, repository, settings, SendMessageUseCase(provider, settings)
            )
            runCurrent()
            vm.onAction(ActiveChatAction.DraftChanged("Pending question"))
            vm.onAction(ActiveChatAction.SendClicked)
            runCurrent()
            assertEquals(1, provider.requests.size)
            repository.delete(conversation.id)
            provider.fail(0, failure)
            runCurrent()
            assertFalse(vm.uiState.value.isGenerating)
            vm.onAction(ActiveChatAction.RetryClicked)
            runCurrent()
            assertEquals(1, provider.requests.size)
            assertEquals(null, repository.find(conversation.id))
        }
    }

    @Test
    fun externallyCompletedHistoryIsReconciledBeforeTheNextSend() = runTest {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val settings = InMemorySettingsRepository()
        settings.saveApiKey("test-only-key")
        val provider = ControlledMentorProvider()
        val vm = ActiveChatViewModel(
            conversation.id, repository, settings, SendMessageUseCase(provider, settings)
        )
        runCurrent()
        vm.onAction(ActiveChatAction.DraftChanged("Question"))
        vm.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        val committed = requireNotNull(repository.find(conversation.id)).append(
            Message(MessageId("external-model"), Role.MODEL, "External answer", 1L)
        )
        repository.save(committed)
        provider.succeed(0, "Obsolete answer")
        runCurrent()
        assertEquals(ChatRequestState.Idle, vm.uiState.value.requestState)
        vm.onAction(ActiveChatAction.DraftChanged("Follow-up"))
        vm.onAction(ActiveChatAction.SendClicked)
        runCurrent()
        assertEquals(2, provider.requests.size)
        assertEquals(
            listOf("Question", "External answer", "Follow-up"),
            provider.requests[1].history.map { it.content }
        )
        provider.succeed(1, "Fresh answer")
        runCurrent()
        assertEquals(ChatRequestState.Idle, vm.uiState.value.requestState)
        assertEquals(4, repository.find(conversation.id)?.messages?.size)
    }
}
