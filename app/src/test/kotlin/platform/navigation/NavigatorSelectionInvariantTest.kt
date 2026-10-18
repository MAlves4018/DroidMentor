package platform.navigation

import domain.repository.InMemoryConversationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import ui.navigation.NavigationAction

/** Each surviving chat route must agree with the repository active selection. */
class NavigatorSelectionInvariantTest {
    @Test fun backFromBToAReactivatesAAndPreservesResume() {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)
        nav.handle(NavigationAction.NewChat)
        val a = nav.currentConversationId!!
        nav.handle(NavigationAction.NewChat)
        val b = nav.currentConversationId!!
        nav.back()
        assertEquals(DroidMentorDestination.ActiveChat(a), nav.currentDestination)
        assertEquals(a, repo.activeId)
        assertEquals(a, repo.resumableId())
        assertEquals(2, repo.conversations.size)
        assertFalse(a == b)
    }

    @Test fun backFromChatToSettingsRestoresChatUnderneath() {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)
        nav.handle(NavigationAction.NewChat)
        val a = nav.currentConversationId!!
        nav.handle(NavigationAction.OpenSettings)
        nav.handle(NavigationAction.NewChat)
        nav.back()
        assertEquals(DroidMentorDestination.Settings, nav.currentDestination)
        assertEquals(a, repo.activeId)
        nav.back()
        assertEquals(DroidMentorDestination.ActiveChat(a), nav.currentDestination)
        assertEquals(a, repo.activeId)
    }

    @Test fun deletingForegroundChatReactivatesSurvivingPreviousChat() {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)
        nav.handle(NavigationAction.NewChat)
        val a = nav.currentConversationId!!
        nav.handle(NavigationAction.NewChat)
        val b = nav.currentConversationId!!
        repo.delete(b)
        nav.removeConversation(b)
        assertEquals(DroidMentorDestination.ActiveChat(a), nav.currentDestination)
        assertEquals(a, repo.activeId)
        assertNull(repo.find(b))
        assertEquals(a, repo.resumableId())
    }

    @Test fun deletingBackgroundChatKeepsCurrentChatActive() {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)
        nav.handle(NavigationAction.NewChat)
        val a = nav.currentConversationId!!
        nav.handle(NavigationAction.NewChat)
        val b = nav.currentConversationId!!
        repo.delete(a)
        nav.removeConversation(a)
        assertEquals(DroidMentorDestination.ActiveChat(b), nav.currentDestination)
        assertEquals(b, repo.activeId)
        nav.back()
        assertEquals(DroidMentorDestination.Title, nav.currentDestination)
        assertNull(repo.activeId)
    }
}
