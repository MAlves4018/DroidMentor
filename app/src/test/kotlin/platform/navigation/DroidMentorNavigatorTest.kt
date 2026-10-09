package platform.navigation

import domain.model.identity.ConversationId
import org.junit.Assert.*
import org.junit.Test
import ui.navigation.NavigationAction

class DroidMentorNavigatorTest {

    @Test
    fun startsAtTitle() {
        val navigator = DroidMentorNavigator()

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )
        assertFalse(navigator.canGoBack)
        assertNull(navigator.currentConversationId)
    }

    @Test
    fun newChatCreatesUniqueConversationIds() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.NewChat)
        val firstId = navigator.currentConversationId

        navigator.handle(NavigationAction.NewChat)
        val secondId = navigator.currentConversationId

        assertNotNull(firstId)
        assertNotNull(secondId)
        assertNotEquals(firstId, secondId)
    }

    @Test
    fun openingHistoryKeepsCurrentChat() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.NewChat)
        val originalDestination = navigator.currentDestination

        navigator.handle(NavigationAction.OpenHistory)

        assertTrue(navigator.isHistoryOpen)
        assertEquals(
            originalDestination,
            navigator.currentDestination
        )
    }

    @Test
    fun backClosesHistoryBeforeLeavingChat() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.NewChat)
        val chatDestination = navigator.currentDestination

        navigator.handle(NavigationAction.OpenHistory)
        navigator.handle(NavigationAction.Back)

        assertFalse(navigator.isHistoryOpen)
        assertEquals(
            chatDestination,
            navigator.currentDestination
        )
    }

    @Test
    fun secondBackReturnsToTitle() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.NewChat)
        navigator.handle(NavigationAction.OpenHistory)

        navigator.back()
        navigator.back()

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )
    }

    @Test
    fun selectingCurrentConversationDoesNotDuplicateRoute() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.NewChat)
        val id = navigator.currentConversationId!!

        navigator.handle(NavigationAction.OpenHistory)
        navigator.handle(
            NavigationAction.OpenConversation(id)
        )

        assertFalse(navigator.isHistoryOpen)
        assertEquals(id, navigator.currentConversationId)

        navigator.back()

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )
    }

    @Test
    fun selectingExistingConversationPreservesId() {
        val navigator = DroidMentorNavigator()
        val id = ConversationId("conversation-123")

        navigator.handle(
            NavigationAction.OpenConversation(id)
        )

        assertEquals(id, navigator.currentConversationId)

        navigator.handle(NavigationAction.OpenSettings)
        assertNull(navigator.currentConversationId)

        navigator.back()

        assertEquals(id, navigator.currentConversationId)
    }

    @Test
    fun selectingPreviousConversationReusesRoute() {
        val navigator = DroidMentorNavigator()

        val firstId = ConversationId("chat-1")
        val secondId = ConversationId("chat-2")

        navigator.handle(
            NavigationAction.OpenConversation(firstId)
        )
        navigator.handle(
            NavigationAction.OpenConversation(secondId)
        )
        navigator.handle(
            NavigationAction.OpenConversation(firstId)
        )

        assertEquals(firstId, navigator.currentConversationId)

        navigator.back()

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )
    }

    @Test
    fun titleClearsNavigationHistoryAndDrawer() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.NewChat)
        navigator.handle(NavigationAction.OpenHistory)
        navigator.handle(NavigationAction.OpenTitle)

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )
        assertFalse(navigator.isHistoryOpen)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun settingsAndAboutReturnToPreviousScreen() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.OpenSettings)

        assertEquals(
            DroidMentorDestination.Settings,
            navigator.currentDestination
        )

        navigator.back()

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )

        navigator.handle(NavigationAction.OpenAbout)

        assertEquals(
            DroidMentorDestination.About,
            navigator.currentDestination
        )

        navigator.back()

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )
    }

    @Test
    fun navigatingToSettingsClosesDrawer() {
        val navigator = DroidMentorNavigator()

        navigator.handle(NavigationAction.OpenHistory)
        navigator.handle(NavigationAction.OpenSettings)

        assertFalse(navigator.isHistoryOpen)
        assertEquals(
            DroidMentorDestination.Settings,
            navigator.currentDestination
        )
    }

    @Test
    fun backAtTitleDoesNothing() {
        val navigator = DroidMentorNavigator()

        navigator.back()

        assertEquals(
            DroidMentorDestination.Title,
            navigator.currentDestination
        )
        assertFalse(navigator.canGoBack)
    }
}
