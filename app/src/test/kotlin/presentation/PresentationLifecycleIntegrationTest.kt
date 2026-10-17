package presentation

import androidx.lifecycle.ViewModelProvider
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import platform.navigation.ChatViewModelScopes
import test.fakes.FakeConversationRepository
import ui.chat.ActiveChatAction
import ui.chat.ActiveChatViewModel
import ui.historyChat.ChatHistoryAction
import ui.historyChat.HistoryEffect
import ui.historyChat.HistoryViewModel

/** Exercises the exact History Delete -> parent route/scope cleanup seam. */
class PresentationLifecycleIntegrationTest {
    private fun model(
        scopes: ChatViewModelScopes,
        repo: FakeConversationRepository,
        id: ConversationId,
    ): ActiveChatViewModel = ViewModelProvider(
        scopes.ownerFor(id),
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ActiveChatViewModel(id, repo, newMessageId = { MessageId("msg-${id.value}") }) as T
        },
    )[ActiveChatViewModel::class.java]

    @Test fun deletingA_releasesItsModelWithoutLosingBDraft() {
        val repo = FakeConversationRepository()
        val a = repo.create().id
        val b = repo.create().id
        val scopes = ChatViewModelScopes()
        val vmA = model(scopes, repo, a)
        val vmB = model(scopes, repo, b)
        vmA.onAction(ActiveChatAction.DraftChanged("Draft A"))
        vmB.onAction(ActiveChatAction.DraftChanged("Draft B"))

        val history = HistoryViewModel(repo)
        history.onAction(ChatHistoryAction.RequestDeleteConversation(a.value))
        val effect = history.onAction(ChatHistoryAction.ConfirmDeleteConversation)
        assertEquals(HistoryEffect.ConversationDeleted(a), effect)
        scopes.release((effect as HistoryEffect.ConversationDeleted).id)
        assertFalse(scopes.isTracked(a))
        assertTrue(scopes.isTracked(b))
        assertEquals("Draft B", model(scopes, repo, b).uiState.value.draft)
    }

    @Test fun deleteA_thenCreateAnotherChat_neverReusesOldOwner() {
        val repo = FakeConversationRepository()
        val a = repo.create().id
        val scopes = ChatViewModelScopes()
        val ownerA = scopes.ownerFor(a)
        model(scopes, repo, a).onAction(ActiveChatAction.DraftChanged("Obsolete draft"))

        val history = HistoryViewModel(repo)
        history.onAction(ChatHistoryAction.RequestDeleteConversation(a.value))
        val effect = history.onAction(ChatHistoryAction.ConfirmDeleteConversation)
        scopes.release((effect as HistoryEffect.ConversationDeleted).id)
        val c = repo.create().id
        val ownerC = scopes.ownerFor(c)
        assertFalse(ownerA === ownerC)
        assertEquals("", model(scopes, repo, c).uiState.value.draft)
        assertEquals(1, repo.conversations.size)
    }
}
