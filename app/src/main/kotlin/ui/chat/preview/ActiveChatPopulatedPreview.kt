package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme
import ui.preview.PreviewFixtures

@Preview(name = "Active Chat - Populated", showBackground = true)
@Composable
fun ActiveChatPopulatedPreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.populated,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
