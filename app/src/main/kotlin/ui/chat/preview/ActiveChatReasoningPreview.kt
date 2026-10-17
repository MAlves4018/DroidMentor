package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.chat.model.ReasoningLevelUi
import ui.theme.DroidMentorTheme

@Preview(name = "Chat · reasoning enabled", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun ActiveChatReasoningPreview() {
    DroidMentorTheme(darkTheme = false) {
        ActiveChatScreen(
            state = ActiveChatUiState(
                draft = "How does StateFlow work?",
                supportedReasoningLevels = ReasoningLevelUi.entries.toSet(),
                reasoningLevel = ReasoningLevelUi.MEDIUM,
            ),
            onAction = {}, onOpenHistory = {}, onOpenSettings = {}, onCopyText = {},
        )
    }
}
