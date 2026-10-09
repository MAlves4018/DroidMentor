package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme
import ui.title.TitleScreen

@Preview(
    name = "Title - Empty",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
)
@Composable
fun TitleScreenPreview() {
    DroidMentorTheme(darkTheme = false) {
        TitleScreen(
            onNewChat = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onOpenAbout = {},
        )
    }
}
