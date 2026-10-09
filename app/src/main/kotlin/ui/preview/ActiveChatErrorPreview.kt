package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.chat.ActiveChatScreen
import ui.theme.DroidMentorTheme

@Preview(name = "Active Chat - Error", showBackground = true)
@Composable
fun ActiveChatErrorPreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.error,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
