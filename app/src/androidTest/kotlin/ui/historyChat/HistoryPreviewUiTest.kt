package ui.historyChat

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import ui.historyChat.preview.HistoryPreviewFixtures
import ui.theme.DroidMentorTheme

/** Verifies that the same immutable fixtures used by the previews render distinct states. */
class HistoryPreviewUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun populatedPreviewRendersKnownConversations() {
        render(HistoryPreviewFixtures.populated)
        composeRule.onNodeWithText("Jetpack Compose Navigation").assertExists()
        composeRule.onNodeWithText("Understanding Kotlin Coroutines").assertExists()
    }

    @Test fun emptyPreviewRendersEmptyNotice() {
        render(HistoryPreviewFixtures.empty)
        composeRule.onNodeWithText("No conversations yet.").assertExists()
    }

    @Test fun loadingPreviewRendersProgressNotice() {
        render(HistoryPreviewFixtures.loading)
        composeRule.onNodeWithText("Loading conversations...").assertExists()
    }

    @Test fun errorPreviewRendersErrorNotice() {
        render(HistoryPreviewFixtures.error)
        composeRule.onNodeWithText("Unable to load conversations.").assertExists()
    }

    private fun render(state: HistoryUiState) {
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
                )
            }
        }
    }
}
