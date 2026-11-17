package ui.chat

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import ui.historyChat.ChatHistoryScreen
import ui.historyChat.preview.HistoryPreviewFixtures
import ui.theme.DroidMentorTheme
import ui.title.TitleScreen
import ui.title.TitleUiState

/** Mandatory-product surfaces do not advertise unavailable optional actions. */
class MandatoryUiAffordanceTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun titleDoesNotExposeNonFunctionalAttachmentButton() {
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                TitleScreen(state = TitleUiState(), onAction = {})
            }
        }
        composeRule.onNodeWithContentDescription("Add file").assertDoesNotExist()
    }

    @Test
    fun mandatoryHistoryHidesUnimplementedSearchAction() {
        val state = HistoryPreviewFixtures.empty
        composeRule.setContent {
            DroidMentorTheme(darkTheme = false) {
                ChatHistoryScreen(
                    groups = state.groups,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onAction = {},
                    onSearch = {},
                    onSettings = {},
                    onAbout = {},
                    showSearch = false,
                )
            }
        }
        composeRule.onNodeWithText("Search chats").assertDoesNotExist()
    }
}
