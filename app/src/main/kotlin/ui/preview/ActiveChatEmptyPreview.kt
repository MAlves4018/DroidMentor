package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.chat.ActiveChatScreen
import ui.theme.DroidMentorTheme

@Preview(name = "Active Chat - Empty", showBackground = true)
@Composable
fun ActiveChatEmptyPreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.empty,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
