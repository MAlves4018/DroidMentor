package platform.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Test
import test.fakes.FakeConversationRepository
import ui.navigation.NavigationAction

/** Regression coverage for Activity recreation and stale saved routes. */
class DroidMentorNavigatorRestorationTest {
    private val testScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    private fun savedRoutes(
        navigator: DroidMentorNavigator,
        repository: FakeConversationRepository,
    ): ArrayList<String> = with(DroidMentorNavigator.saver(repository)) {
        with(testScope) { save(navigator)!! }
    }

    @Test fun restoresSettingsAboveChatAndReturnsToSameChatOnBack() {
        val repository = FakeConversationRepository()
        val before = DroidMentorNavigator(repository = repository)
        before.handle(NavigationAction.NewChat)
        val id = before.currentConversationId!!
        before.handle(NavigationAction.OpenSettings)

        val saver = DroidMentorNavigator.saver(repository)
        val after = saver.restore(savedRoutes(before, repository))!!
        assertEquals(DroidMentorDestination.Settings, after.currentDestination)
        after.back()
        assertEquals(DroidMentorDestination.ActiveChat(id), after.currentDestination)
        assertEquals(id, repository.activeId)
    }

    @Test fun restoredNavigatorCreatesChatInOriginalRepository() {
        val repository = FakeConversationRepository()
        val before = DroidMentorNavigator(repository = repository)
        val saver = DroidMentorNavigator.saver(repository)
        val after = saver.restore(savedRoutes(before, repository))!!
        after.handle(NavigationAction.NewChat)
        assertEquals(after.currentConversationId, repository.activeId)
        assertEquals(1, repository.conversations.size)
    }

    @Test fun ignoresChatDeletedAfterSavingRoutes() {
        val repository = FakeConversationRepository()
        val before = DroidMentorNavigator(repository = repository)
        before.handle(NavigationAction.NewChat)
        val deleted = before.currentConversationId!!
        val saver = DroidMentorNavigator.saver(repository)
        val routes = savedRoutes(before, repository)
        repository.delete(deleted)

        val after = saver.restore(routes)!!
        assertEquals(DroidMentorDestination.Title, after.currentDestination)
        assertFalse(after.canGoBack)
        assertNull(after.currentConversationId)
    }

    @Test fun skipsInvalidAndMissingRoutesDuringRestore() {
        val repository = FakeConversationRepository()
        val saver = DroidMentorNavigator.saver(repository)
        val after = saver.restore(arrayListOf("bad", "chat:missing", "title", "settings"))!!
        assertEquals(DroidMentorDestination.Settings, after.currentDestination)
        after.back()
        assertEquals(DroidMentorDestination.Title, after.currentDestination)
    }

    @Test fun fallsBackToTitleWhenAllSavedRoutesAreInvalid() {
        val repository = FakeConversationRepository()
        val saver = DroidMentorNavigator.saver(repository)
        val after = saver.restore(arrayListOf("bad", "chat:missing", "chat:", "chat:   "))!!
        assertEquals(DroidMentorDestination.Title, after.currentDestination)
        assertFalse(after.canGoBack)
    }

    @Test fun retainedConversationMayBeReopenedAfterRestoration() {
        val repository = FakeConversationRepository()
        val before = DroidMentorNavigator(repository = repository)
        before.handle(NavigationAction.NewChat)
        val id = before.currentConversationId!!
        before.handle(NavigationAction.OpenTitle)
        val saver = DroidMentorNavigator.saver(repository)
        val after = saver.restore(savedRoutes(before, repository))!!
        after.handle(NavigationAction.OpenConversation(id))
        assertEquals(id, after.currentConversationId)
        assertTrue(after.canGoBack)
    }
    @Test fun deletedForegroundChatOnRestore_reselectsPreviousSurvivingChat() {
        val repository = FakeConversationRepository()
        val before = DroidMentorNavigator(repository = repository)
        before.handle(NavigationAction.NewChat)
        val a = before.currentConversationId!!
        before.handle(NavigationAction.NewChat)
        val b = before.currentConversationId!!
        val routes = savedRoutes(before, repository)

        repository.delete(b)
        val after = DroidMentorNavigator.saver(repository).restore(routes)!!
        assertEquals(DroidMentorDestination.ActiveChat(a), after.currentDestination)
        assertEquals(a, repository.activeId)
        assertEquals(a, repository.resumableId())
    }
}
