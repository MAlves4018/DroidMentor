package ui.historyChat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ui.historyChat.components.ConversationGroup
import ui.theme.DroidMentorTheme

/**
 * Instrumented UI tests for the Chat History drawer.
 *
 * Uses fake presentation data and verifies callbacks.
 * Does not require routing, ViewModels or persistence.
 */
class ChatHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val groups = listOf(
        ConversationGroup(
            title = "Today",
            conversations = listOf(
                ConversationSummary(
                    id = "1",
                    title = "Jetpack Compose",
                    lastActivity = "Today"
                )
            )
        ),
        ConversationGroup(
            title = "Yesterday",
            conversations = listOf(
                ConversationSummary(
                    id = "2",
                    title = "Room Database",
                    lastActivity = "Yesterday"
                )
            )
        )
    )

    private fun setScreen(
        groups: List<ConversationGroup> = this.groups,
        isLoading: Boolean = false,
        errorMessage: String? = null,
        onAction: (ChatHistoryAction) -> Unit = {},
        onSearch: () -> Unit = {},
        onSettings: () -> Unit = {},
        onAbout: () -> Unit = {}
    ) {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                ChatHistoryScreen(
                    groups = groups,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onAction = onAction,
                    onSearch = onSearch,
                    onSettings = onSettings,
                    onAbout = onAbout
                )
            }
        }
    }

    @Test
    fun drawerDisplaysMainActions() {
        setScreen()

        composeRule.onNodeWithText("DroidMentor").assertIsDisplayed()
        composeRule.onNodeWithText("New chat").assertIsDisplayed()
        composeRule.onNodeWithText("Search chats").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun drawerDisplaysGroupedConversations() {
        setScreen()

        composeRule.onNodeWithText("TODAY").assertIsDisplayed()
        composeRule.onNodeWithText("YESTERDAY").assertIsDisplayed()
        composeRule.onNodeWithText("Jetpack Compose").assertIsDisplayed()
        composeRule.onNodeWithText("Room Database").assertIsDisplayed()
    }

    @Test
    fun newChatEmitsAction() {
        var receivedAction: ChatHistoryAction? = null

        setScreen(
            onAction = { receivedAction = it }
        )

        composeRule.onNodeWithText("New chat").performClick()

        composeRule.runOnIdle {
            assertEquals(
                ChatHistoryAction.NewConversation,
                receivedAction
            )
        }
    }

    @Test
    fun conversationClickEmitsCorrectId() {
        var receivedAction: ChatHistoryAction? = null

        setScreen(
            onAction = { receivedAction = it }
        )

        composeRule.onNodeWithText("Jetpack Compose").performClick()

        composeRule.runOnIdle {
            assertEquals(
                ChatHistoryAction.OpenConversation("1"),
                receivedAction
            )
        }
    }

    @Test
    fun searchSettingsAndAboutInvokeCallbacks() {
        var searchClicked = false
        var settingsClicked = false
        var aboutClicked = false

        setScreen(
            onSearch = { searchClicked = true },
            onSettings = { settingsClicked = true },
            onAbout = { aboutClicked = true }
        )

        composeRule.onNodeWithText("Search chats").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("About").performClick()

        composeRule.runOnIdle {
            assertTrue(searchClicked)
            assertTrue(settingsClicked)
            assertTrue(aboutClicked)
        }
    }

    @Test
    fun deleteRequiresConfirmation() {
        var receivedAction: ChatHistoryAction? = null

        setScreen(
            groups = listOf(groups.first()),
            onAction = { receivedAction = it }
        )

        // Only one conversation is present, so only one overflow button exists.
        composeRule.onNodeWithText("⋮").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("Delete conversation?")
            .assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(null, receivedAction)
        }

        composeRule.onNodeWithText("Delete").performClick()

        composeRule.runOnIdle {
            assertEquals(
                ChatHistoryAction.DeleteConversation("1"),
                receivedAction
            )
        }
    }

    @Test
    fun cancelDeleteDoesNotEmitAction() {
        var receivedAction: ChatHistoryAction? = null

        setScreen(
            groups = listOf(groups.first()),
            onAction = { receivedAction = it }
        )

        composeRule.onNodeWithText("⋮").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Delete conversation?")
            .assertDoesNotExist()

        composeRule.runOnIdle {
            assertEquals(null, receivedAction)
        }
    }

    @Test
    fun emptyStateDisplaysMessage() {
        setScreen(groups = emptyList())

        composeRule.onNodeWithText("No conversations yet.")
            .assertIsDisplayed()
    }

    @Test
    fun loadingStateDisplaysMessage() {
        setScreen(isLoading = true)

        composeRule.onNodeWithText("Loading conversations...")
            .assertIsDisplayed()
    }

    @Test
    fun errorStateDisplaysMessage() {
        setScreen(errorMessage = "Unable to load conversations.")

        composeRule.onNodeWithText("Unable to load conversations.")
            .assertIsDisplayed()
    }
}