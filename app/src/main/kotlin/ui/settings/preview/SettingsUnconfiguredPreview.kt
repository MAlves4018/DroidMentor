package ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme

@Preview(
    name = "Settings - Unconfigured",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
)
@Composable
fun SettingsUnconfiguredPreview() {
    DroidMentorTheme(darkTheme = false) {
        SettingsScreen(
            state = SettingsUiState(
                page = SettingsPage.API_KEY,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
