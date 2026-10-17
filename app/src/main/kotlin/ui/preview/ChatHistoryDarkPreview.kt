package ui.historyChat.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.historyChat.ChatHistoryScreen
import ui.theme.DroidMentorTheme

@Preview(
    name = "Chat History - Dark",
    widthDp = 316,
    heightDp = 844,
    showBackground = true,
)
@Composable
fun ChatHistoryDarkPreview() {
    val state = HistoryPreviewFixtures.populated
    DroidMentorTheme(darkTheme = true) {
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
