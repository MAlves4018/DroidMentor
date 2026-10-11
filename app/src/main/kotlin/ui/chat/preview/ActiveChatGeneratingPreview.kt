package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme
import ui.preview.PreviewFixtures

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
