package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme
import ui.preview.PreviewFixtures

@Preview(name = "Active Chat - Editing", showBackground = true)
@Composable
fun ActiveChatEditingPreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.editing,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
