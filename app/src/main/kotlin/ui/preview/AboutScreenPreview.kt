package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.about.AboutScreen
import ui.theme.DroidMentorTheme

@Preview(
    name = "About",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
)
@Composable
fun AboutScreenPreview() {
    DroidMentorTheme(darkTheme = false) {
        AboutScreen(onBack = {})
    }
}
