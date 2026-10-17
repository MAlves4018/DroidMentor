package ui.chat

import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import test.fakes.FakeConversationRepository
import test.fixtures.ConversationFixtures
import ui.chat.model.MessageEditState

class ActiveChatViewModelTest {
    @Test fun sendPersistsToRepository_andClearsDraft() {
        val repository = FakeConversationRepository()
        val conversation = repository.create()
        val vm = viewModel(repository, conversation.id, "m1")
        vm.onAction(ActiveChatAction.DraftChanged("  Hello  "))
        vm.onAction(ActiveChatAction.SendClicked)

        assertEquals("Hello", repository.find(conversation.id)!!.messages.single().content)
        assertEquals("m1", vm.uiState.value.messages.single().id)
        assertEquals("", vm.uiState.value.draft)
        assertFalse(vm.uiState.value.canSend)
        assertEquals(listOf(conversation.id), repository.savedIds)
    }

    @Test fun repeatedSendOnlySavesOneMessage() {
        val repository = FakeConversationRepository()
        val id = repository.create().id
        val vm = viewModel(repository, id, "m1")
        vm.onAction(ActiveChatAction.DraftChanged("Once"))
        vm.onAction(ActiveChatAction.SendClicked)
        vm.onAction(ActiveChatAction.SendClicked)
        assertEquals(1, repository.find(id)!!.messages.size)
        assertEquals(1, repository.savedIds.size)
    }

    @Test fun rewriteUpdatesRepository_andDropsTheSuffix() {
        val initial = ConversationFixtures.conversation("A", "First", "Second")
        val repository = FakeConversationRepository(listOf(initial), initial.id)
        val vm = viewModel(repository, initial.id, "unused")
        val firstId = initial.messages.first().id.value
        vm.onAction(ActiveChatAction.EditMessageClicked(firstId))
        vm.onAction(ActiveChatAction.EditDraftChanged("Updated"))
        vm.onAction(ActiveChatAction.SaveEditClicked)
        assertTrue(vm.uiState.value.editState is MessageEditState.ConfirmRewrite)
        vm.onAction(ActiveChatAction.ConfirmRewriteClicked)
        val result = repository.find(initial.id)!!
        assertEquals(1, result.messages.size)
        assertEquals(firstId, result.messages.single().id.value)
        assertEquals("Updated", result.messages.single().content)
        assertEquals(MessageEditState.None, vm.uiState.value.editState)
    }

    @Test fun cancelEditPreservesConversation() {
        val initial = ConversationFixtures.conversation("A", "Original")
        val repository = FakeConversationRepository(listOf(initial), initial.id)
        val vm = viewModel(repository, initial.id, "unused")
        vm.onAction(ActiveChatAction.EditMessageClicked(initial.messages.single().id.value))
        vm.onAction(ActiveChatAction.EditDraftChanged("Different"))
        vm.onAction(ActiveChatAction.CancelEditClicked)
        assertEquals(initial, repository.find(initial.id))
        assertTrue(repository.savedIds.isEmpty())
    }

    @Test fun independentConversationViewModels_doNotShareDraftOrMessages() {
        val repository = FakeConversationRepository()
        val a = repository.create().id
        val b = repository.create().id
        val vmA = viewModel(repository, a, "a1")
        val vmB = viewModel(repository, b, "b1")
        vmA.onAction(ActiveChatAction.DraftChanged("A only"))
        vmA.onAction(ActiveChatAction.SendClicked)
        vmB.onAction(ActiveChatAction.DraftChanged("B draft"))
        assertEquals(1, repository.find(a)!!.messages.size)
        assertTrue(repository.find(b)!!.messages.isEmpty())
        assertEquals("B draft", vmB.uiState.value.draft)
        assertEquals("", vmA.uiState.value.draft)
    }

    @Test fun viewModelReconstructedFromRepository_readsPersistedContent() {
        val repository = FakeConversationRepository()
        val id = repository.create().id
        val first = viewModel(repository, id, "message-1")
        first.onAction(ActiveChatAction.DraftChanged("Persisted in memory"))
        first.onAction(ActiveChatAction.SendClicked)
        val reopened = viewModel(repository, id, "message-2")
        assertEquals("Persisted in memory", reopened.uiState.value.messages.single().content)
    }

    @Test fun draftRemainsOnSameViewModelInstance_withoutComposerBeingMounted() {
        val repository = FakeConversationRepository()
        val id = repository.create().id
        val vm = viewModel(repository, id, "unused")
        vm.onAction(ActiveChatAction.DraftChanged("In progress"))
        assertEquals("In progress", vm.uiState.value.draft)
        // UI no longer owns this value via Compose remember().
        assertTrue(repository.find(id)!!.messages.isEmpty())
    }

    @Test fun missingConversation_isRejectedBeforeCreatingViewModelState() {
        val repository = FakeConversationRepository()
        val missingId = ConversationId("missing")
        val error = runCatching {
            viewModel(repository, missingId, "unused")
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(repository.conversations.isEmpty())
    }

    @Test fun deletedConversation_isRejectedOnNewViewModelCreation() {
        val repository = FakeConversationRepository()
        val id = repository.create().id
        repository.delete(id)
        val error = runCatching { viewModel(repository, id, "unused") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(repository.conversations.isEmpty())
    }

    private fun viewModel(
        repository: FakeConversationRepository,
        id: ConversationId,
        newMessageId: String,
    ) = ActiveChatViewModel(
        conversationId = id,
        repository = repository,
        newMessageId = { MessageId(newMessageId) },
    )
}
