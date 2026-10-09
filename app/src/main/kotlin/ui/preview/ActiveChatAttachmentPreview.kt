package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.chat.ActiveChatScreen
import ui.theme.DroidMentorTheme

@Preview(name = "Active Chat - Attachment + Reasoning", showBackground = true)
@Composable
fun ActiveChatAttachmentPreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.attachment,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
