package ui.title

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ui.theme.DroidMentorTheme

class TitleScreenUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun exposesAllMainEntryActions() {
        setScreen()
        composeRule.onNodeWithContentDescription("Open chat history").assertExists()
        composeRule.onNodeWithContentDescription("New chat").assertExists()
        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithText("About").assertExists()
    }

    @Test fun entryActionsEmitTypedIntents() {
        val actions = mutableListOf<TitleAction>()
        setScreen(onAction = { actions += it })
        composeRule.onNodeWithContentDescription("New chat").performClick()
        composeRule.onNodeWithContentDescription("Open chat history").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("About").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(
                TitleAction.NewChatClicked,
                TitleAction.OpenHistoryClicked,
                TitleAction.OpenSettingsClicked,
                TitleAction.OpenAboutClicked,
            ), actions)
        }
    }

    @Test fun typingEmitsDraftChangeWithoutOwningState() {
        val actions = mutableListOf<TitleAction>()
        setScreen(onAction = { actions += it })
        composeRule.onNode(hasSetTextAction()).performTextInput("Hello")
        composeRule.runOnIdle {
            assertEquals(TitleAction.DraftChanged("Hello"), actions.last())
        }
    }

    private fun setScreen(
        state: TitleUiState = TitleUiState(),
        onAction: (TitleAction) -> Unit = {},
    ) {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                TitleScreen(state = state, onAction = onAction)
            }
        }
    }
}
