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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertTrue
import ui.chat.ActiveChatTestTags

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

        composeRule.setContent {
            var state by remember {
                mutableStateOf(TitleUiState())
            }

            DroidMentorTheme(darkTheme = false) {
                TitleScreen(
                    state = state,
                    onAction = { action ->
                        actions += action

                        if (action is TitleAction.DraftChanged) {
                            state = state.copy(
                                draft = action.value
                            )
                        }
                    }
                )
            }
        }

        composeRule
            .onNodeWithTag(ActiveChatTestTags.COMPOSER_FIELD)
            .performTextInput("Hello")

        composeRule
            .onNodeWithTag(ActiveChatTestTags.COMPOSER_FIELD)
            .assertTextEquals("Hello")

        composeRule.runOnIdle {
            assertTrue(
                actions.contains(
                    TitleAction.DraftChanged("Hello")
                )
            )
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
