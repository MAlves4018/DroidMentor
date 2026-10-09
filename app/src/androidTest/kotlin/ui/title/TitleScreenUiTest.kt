package ui.title

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ui.theme.DroidMentorTheme

class TitleScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exposesAllMainEntryActions() {
        setScreen()

        composeRule
            .onNodeWithContentDescription("Open chat history")
            .assertExists()

        composeRule
            .onNodeWithContentDescription("New chat")
            .assertExists()

        composeRule
            .onNodeWithText("Settings")
            .assertExists()

        composeRule
            .onNodeWithText("About")
            .assertExists()
    }

    @Test
    fun entryActionsInvokeTheirCallbacks() {
        var newChat = 0
        var history = 0
        var settings = 0
        var about = 0

        setScreen(
            onNewChat = { newChat++ },
            onOpenHistory = { history++ },
            onOpenSettings = { settings++ },
            onOpenAbout = { about++ },
        )

        composeRule
            .onNodeWithContentDescription("New chat")
            .performClick()

        composeRule
            .onNodeWithContentDescription("Open chat history")
            .performClick()

        composeRule
            .onNodeWithText("Settings")
            .performClick()

        composeRule
            .onNodeWithText("About")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, newChat)
            assertEquals(1, history)
            assertEquals(1, settings)
            assertEquals(1, about)
        }
    }

    private fun setScreen(
        onNewChat: () -> Unit = {},
        onOpenHistory: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onOpenAbout: () -> Unit = {},
    ) {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                TitleScreen(
                    onNewChat = onNewChat,
                    onOpenHistory = onOpenHistory,
                    onOpenSettings = onOpenSettings,
                    onOpenAbout = onOpenAbout,
                )
            }
        }
    }
}
