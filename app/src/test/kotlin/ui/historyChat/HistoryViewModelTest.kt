package ui.historyChat

import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import test.fakes.FakeConversationRepository
import test.fixtures.ConversationFixtures
import ui.navigation.NavigationAction

class HistoryViewModelTest {
    @Test fun emptyRepository_showsEmptyWithoutFailure() {
        val vm = HistoryViewModel(FakeConversationRepository())
        assertTrue(vm.uiState.value.groups.single().conversations.isEmpty())
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun populatedRepository_usesNewestFirstAndActiveLabel() {
        val a = ConversationFixtures.conversation("A", "First question")
        val b = ConversationFixtures.conversation("B", "Second question")
        val repo = FakeConversationRepository(listOf(b, a), a.id)
        val summaries = HistoryViewModel(repo).uiState.value.groups.single().conversations
        assertEquals(listOf("B", "A"), summaries.map { it.id })
        assertEquals(listOf("Saved", "Active"), summaries.map { it.lastActivity })
        assertEquals("Second question", summaries.first().title)
    }

    @Test fun externalRepositoryUpdate_isProjectedAfterRefresh() {
        val repo = FakeConversationRepository()
        val vm = HistoryViewModel(repo)
        val a = repo.create()
        repo.save(ConversationFixtures.conversation(a.id.value, "New text"))
        vm.refresh()
        assertEquals("New text", vm.uiState.value.groups.single().conversations.single().title)
        assertEquals("Active", vm.uiState.value.groups.single().conversations.single().lastActivity)
    }

    @Test fun openExisting_returnsNavigationEffect_withoutMutatingSelection() {
        val a = ConversationFixtures.conversation("A")
        val b = ConversationFixtures.conversation("B")
        val repo = FakeConversationRepository(listOf(a, b), b.id)
        val effect = HistoryViewModel(repo).onAction(ChatHistoryAction.OpenConversation("A"))
        assertEquals(HistoryEffect.Navigate(NavigationAction.OpenConversation(a.id)), effect)
        assertEquals(b.id, repo.activeId) // Navigator remains the activation owner.
    }

    @Test fun openMissing_hasNoEffect() {
        val repo = FakeConversationRepository()
        assertNull(HistoryViewModel(repo).onAction(ChatHistoryAction.OpenConversation("missing")))
    }

    @Test fun deleteInactive_removesOnlyTargetAndEmitsCleanup() {
        val a = ConversationFixtures.conversation("A", "A question")
        val b = ConversationFixtures.conversation("B", "B question")
        val repo = FakeConversationRepository(listOf(a, b), b.id)
        val vm = HistoryViewModel(repo)
        assertEquals(HistoryEffect.ConversationDeleted(a.id), confirmDelete(vm, "A"))
        assertEquals(listOf(b), repo.conversations)
        assertEquals(b.id, repo.activeId)
        assertEquals(listOf(a.id), repo.deletedIds)

        assertEquals(listOf("B"), vm.uiState.value.groups.single().conversations.map { it.id })
    }

    @Test fun deleteActive_clearsSelectionAndKeepsOtherConversation() {
        val a = ConversationFixtures.conversation("A")
        val b = ConversationFixtures.conversation("B")
        val repo = FakeConversationRepository(listOf(a, b), a.id)
        val vm = HistoryViewModel(repo)
        assertEquals(HistoryEffect.ConversationDeleted(a.id), confirmDelete(vm, "A"))
        assertNull(repo.activeId)
        assertEquals(listOf(b), repo.conversations)
        assertEquals("Saved", vm.uiState.value.groups.single().conversations.single().lastActivity)
        assertEquals("Saved", vm.uiState.value.groups.single().conversations.single().lastActivity)
    }

    @Test fun invalidConversationIds_areRejectedWithoutThrowing() {
        val vm = HistoryViewModel(FakeConversationRepository())
        assertNull(vm.onAction(ChatHistoryAction.OpenConversation(" ")))
        assertEquals("Unable to open conversation.", vm.uiState.value.errorMessage)
        assertNull(confirmDelete(vm, " "))
        assertEquals("Unable to delete conversation.", vm.uiState.value.errorMessage)
    }

    @Test fun deleteMissing_doesNotCallRepositoryDelete() {
        val repo = FakeConversationRepository()
        val vm = HistoryViewModel(repo)
        assertNull(confirmDelete(vm, "absent"))
        assertTrue(repo.deletedIds.isEmpty())
    }

    @Test fun navigationActions_areMappedToRouterWithoutSideEffects() {
        val repo = FakeConversationRepository()
        val vm = HistoryViewModel(repo)
        assertEquals(HistoryEffect.Navigate(NavigationAction.NewChat), vm.onAction(ChatHistoryAction.NewConversation))
        assertEquals(HistoryEffect.Navigate(NavigationAction.OpenSettings), vm.onAction(ChatHistoryAction.OpenSettings))
        assertEquals(HistoryEffect.Navigate(NavigationAction.OpenAbout), vm.onAction(ChatHistoryAction.OpenAbout))
        assertEquals(HistoryEffect.Navigate(NavigationAction.OpenTitle), vm.onAction(ChatHistoryAction.OpenTitle))
        assertTrue(repo.conversations.isEmpty())
    }

    @Test fun refreshTracksExternalActivation() {
        val a = ConversationFixtures.conversation("A")
        val b = ConversationFixtures.conversation("B")
        val repo = FakeConversationRepository(listOf(a, b), a.id)
        val vm = HistoryViewModel(repo)
        assertTrue(repo.activate(b.id))
        vm.refresh()
        assertEquals(listOf("Saved", "Active"), vm.uiState.value.groups.single().conversations.map { it.lastActivity })
    }

    @Test fun repositoryReadFailure_setsExplicitErrorState() {
        val failed = object : ConversationRepository by FakeConversationRepository() {
            override val conversations: List<domain.model.Conversation>
                get() = error("Storage unavailable")
        }
        val vm = HistoryViewModel(failed)
        assertEquals("Unable to load conversations.", vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.groups.isEmpty())
    }

    @Test fun deleteFailure_keepsConversationAndReportsError() {
        val a = ConversationFixtures.conversation("A")
        val repository = FakeConversationRepository(listOf(a), a.id)
        val failed = object : ConversationRepository by repository {
            override fun delete(id: ConversationId) { error("Delete failed") }
        }
        val vm = HistoryViewModel(failed)
        assertNull(confirmDelete(vm, "A"))
        assertEquals("Unable to delete conversation.", vm.uiState.value.errorMessage)
        assertEquals(listOf(a), repository.conversations)
    }

    @Test fun truncatesLongTitlesToFortyCharacters() {
        val a = ConversationFixtures.conversation("A", "x".repeat(50))
        val vm = HistoryViewModel(FakeConversationRepository(listOf(a)))
        assertEquals("x".repeat(40), vm.uiState.value.groups.single().conversations.single().title)
    }

    private fun confirmDelete(vm: HistoryViewModel, conversationId: String): HistoryEffect? {
        assertNull(vm.onAction(ChatHistoryAction.RequestDeleteConversation(conversationId)))
        return vm.onAction(ChatHistoryAction.ConfirmDeleteConversation)
    }

    @Test fun requestDelete_doesNotDeleteUntilConfirmed() {
        val a = ConversationFixtures.conversation("A", "Important conversation")
        val repo = FakeConversationRepository(listOf(a))
        val vm = HistoryViewModel(repo)
        assertNull(vm.onAction(ChatHistoryAction.ConfirmDeleteConversation))
        assertNull(vm.onAction(ChatHistoryAction.RequestDeleteConversation("A")))
        assertEquals("A", vm.uiState.value.pendingDeletion?.id)
        assertEquals("Important conversation", vm.uiState.value.pendingDeletion?.title)
        assertEquals(listOf(a), repo.conversations)
        assertTrue(repo.deletedIds.isEmpty())
        assertEquals(HistoryEffect.ConversationDeleted(a.id), vm.onAction(ChatHistoryAction.ConfirmDeleteConversation))
        assertNull(vm.uiState.value.pendingDeletion)
        assertNull(vm.onAction(ChatHistoryAction.ConfirmDeleteConversation))
        assertEquals(listOf(a.id), repo.deletedIds)
    }

    @Test fun cancelDelete_preservesConversationAndClearsPrompt() {
        val a = ConversationFixtures.conversation("A")
        val repo = FakeConversationRepository(listOf(a))
        val vm = HistoryViewModel(repo)
        vm.onAction(ChatHistoryAction.RequestDeleteConversation("A"))
        vm.onAction(ChatHistoryAction.CancelDeleteConversation)
        assertNull(vm.uiState.value.pendingDeletion)
        assertNull(vm.onAction(ChatHistoryAction.ConfirmDeleteConversation))
        assertTrue(repo.deletedIds.isEmpty())
    }

    @Test fun refresh_keepsValidPromptButDropsExternallyDeletedConversation() {
        val a = ConversationFixtures.conversation("A")
        val repo = FakeConversationRepository(listOf(a))
        val vm = HistoryViewModel(repo)
        vm.onAction(ChatHistoryAction.RequestDeleteConversation("A"))
        vm.refresh()
        assertEquals("A", vm.uiState.value.pendingDeletion?.id)
        repo.delete(a.id)
        vm.refresh()
        assertNull(vm.uiState.value.pendingDeletion)
        assertNull(vm.onAction(ChatHistoryAction.ConfirmDeleteConversation))
    }

    @Test fun requestOfUnknownChatNeverDeletesExistingChat() {
        val a = ConversationFixtures.conversation("A")
        val repo = FakeConversationRepository(listOf(a))
        val vm = HistoryViewModel(repo)
        assertNull(vm.onAction(ChatHistoryAction.RequestDeleteConversation("missing")))
        assertNull(vm.uiState.value.pendingDeletion)
        assertNull(vm.onAction(ChatHistoryAction.ConfirmDeleteConversation))
        assertEquals(listOf(a), repo.conversations)
    }
}
