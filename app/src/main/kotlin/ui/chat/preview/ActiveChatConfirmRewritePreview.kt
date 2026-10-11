package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme
import ui.preview.PreviewFixtures

@Preview(name = "Active Chat - Confirm Rewrite", showBackground = true)
@Composable
fun ActiveChatConfirmRewritePreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = PreviewFixtures.confirmRewrite,
            onAction = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCopyText = {},
        )
    }
}
