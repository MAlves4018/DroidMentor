package ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme

@Preview(name = "Settings · overview dark", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SettingsOverviewDarkPreview() {
    DroidMentorTheme(darkTheme = true) {
        SettingsScreen(state = SettingsUiState(apiKeyConfigured = true),
            isDarkTheme = true, onAction = {}, onBack = {})
    }
}
