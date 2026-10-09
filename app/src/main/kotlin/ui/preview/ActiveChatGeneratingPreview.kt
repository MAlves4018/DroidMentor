package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.chat.ActiveChatScreen
import ui.theme.DroidMentorTheme

@Preview(name = "Active Chat - Generating", showBackground = true)
@Composable
fun ActiveChatGeneratingPreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.generating,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
