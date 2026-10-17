package ui.historyChat.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.historyChat.ChatHistoryScreen
import ui.theme.DroidMentorTheme

@Preview(
    name = "Chat History - Empty",
    widthDp = 316,
    heightDp = 844,
    showBackground = true,
)
@Composable
fun ChatHistoryEmptyPreview() {
    val state = HistoryPreviewFixtures.empty
    DroidMentorTheme(darkTheme = false) {
        ChatHistoryScreen(
            groups = state.groups,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            onAction = {},
            onSearch = {},
            onSettings = {},
            onAbout = {},
            onTitleClick = {},
        )
    }
}
