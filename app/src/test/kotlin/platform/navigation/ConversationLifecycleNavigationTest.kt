package platform.navigation

import domain.repository.InMemoryConversationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import ui.navigation.NavigationAction

class ConversationLifecycleNavigationTest {

    @Test
    fun newChatCreatesRepositoryEntryAndOpensItsRoute() = runTest {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)

        nav.handle(NavigationAction.NewChat)

        assertEquals(repo.activeId, nav.currentConversationId)
        assertEquals(1, repo.conversations.size)
    }

    @Test
    fun openingHistoryThenSelectingOldChatUpdatesSelection() = runTest {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)

        nav.handle(NavigationAction.NewChat)
        val old = nav.currentConversationId!!

        nav.handle(NavigationAction.NewChat)
        nav.handle(NavigationAction.OpenHistory)

        assertTrue(nav.isHistoryOpen)

        nav.handle(NavigationAction.OpenConversation(old))

        assertFalse(nav.isHistoryOpen)
        assertEquals(old, nav.currentConversationId)
        assertEquals(old, repo.activeId)
        assertEquals(2, repo.conversations.size)
    }

    @Test
    fun openingMissingChatDoesNotChangeRouteOrSelection() = runTest {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)

        nav.handle(NavigationAction.NewChat)
        val previous = nav.currentConversationId

        nav.handle(
            NavigationAction.OpenConversation(
                domain.model.identity.ConversationId("missing")
            )
        )

        assertEquals(previous, nav.currentConversationId)
        assertEquals(previous, repo.activeId)
    }

    @Test
    fun returningToTitleClearsResumeButKeepsHistory() = runTest {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)

        nav.handle(NavigationAction.NewChat)
        nav.handle(NavigationAction.OpenTitle)

        assertEquals(
            DroidMentorDestination.Title,
            nav.currentDestination
        )
        assertNull(repo.resumableId())
        assertEquals(1, repo.conversations.size)
    }

    @Test
    fun backFromChatClearsResumeButKeepsConversation() = runTest {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)

        nav.handle(NavigationAction.NewChat)
        nav.back()

        assertEquals(
            DroidMentorDestination.Title,
            nav.currentDestination
        )
        assertNull(repo.activeId)
        assertEquals(1, repo.conversations.size)
    }

    @Test
    fun deletingActiveConversationAndRemovingRouteReturnsToTitle() = runTest {
        val repo = InMemoryConversationRepository()
        val nav = DroidMentorNavigator(repository = repo)

        nav.handle(NavigationAction.NewChat)
        val id = nav.currentConversationId!!

        repo.delete(id)
        nav.removeConversation(id)

        assertEquals(
            DroidMentorDestination.Title,
            nav.currentDestination
        )
        assertNull(repo.resumableId())
        assertTrue(repo.conversations.isEmpty())
    }

    @Test
    fun recreatedNavigatorCanResumeUsingSameRepository() = runTest {
        val repo = InMemoryConversationRepository()
        val first = DroidMentorNavigator(repository = repo)

        first.handle(NavigationAction.NewChat)

        val id = repo.resumableId()!!

        val resumed = DroidMentorNavigator(
            initialBackStack = listOf(
                DroidMentorDestination.Title,
                DroidMentorDestination.ActiveChat(id)
            ),
            repository = repo
        )

        assertEquals(id, resumed.currentConversationId)
    }
}
