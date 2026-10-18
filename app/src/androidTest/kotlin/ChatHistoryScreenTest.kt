package ui.historyChat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
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
        pendingDeletion: () -> ConversationSummary? = { null },
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
                    pendingDeletion = pendingDeletion(),
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
    fun deleteRequestDoesNotDeleteUntilStateDisplaysConfirmation() {
        var pending by mutableStateOf<ConversationSummary?>(null)
        val actions = mutableListOf<ChatHistoryAction>()
        val summary = groups.first().conversations.first()
        setScreen(
            groups = listOf(groups.first()),
            pendingDeletion = { pending },
            onAction = { action ->
                actions += action
                if (action is ChatHistoryAction.RequestDeleteConversation) pending = summary
            },
        )
        composeRule.onNodeWithContentDescription("Conversation options").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete conversation?").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(ChatHistoryAction.RequestDeleteConversation("1"), actions.first())
            assertEquals(1, actions.size) // A request cannot delete anything by itself.
        }
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.runOnIdle {
            assertEquals(ChatHistoryAction.ConfirmDeleteConversation, actions.last())
        }
    }

    @Test
    fun cancelDeleteOnlyEmitsCancelIntent() {
        val actions = mutableListOf<ChatHistoryAction>()
        var pending by mutableStateOf<ConversationSummary?>(groups.first().conversations.first())
        setScreen(
            groups = listOf(groups.first()),
            pendingDeletion = { pending },
            onAction = { action ->
                actions += action
                if (action == ChatHistoryAction.CancelDeleteConversation) pending = null
            },
        )
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Delete conversation?").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(listOf(ChatHistoryAction.CancelDeleteConversation), actions)
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