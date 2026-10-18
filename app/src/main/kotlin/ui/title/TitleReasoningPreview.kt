package ui.title

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.chat.model.ReasoningLevelUi
import ui.theme.DroidMentorTheme

@Preview(name = "Title · reasoning enabled", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun TitleReasoningPreview() {
    DroidMentorTheme(darkTheme = false) {
        TitleScreen(
            state = TitleUiState(
                draft = "Explain Kotlin coroutines",
                supportedReasoningLevels = ReasoningLevelUi.entries.toSet(),
            ),
            onAction = {},
        )
    }
}
