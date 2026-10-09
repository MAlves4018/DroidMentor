package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.chat.ActiveChatScreen
import ui.theme.DroidMentorTheme

@Preview(name = "Active Chat - Offline", showBackground = true)
@Composable
fun ActiveChatOfflinePreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.offline,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
