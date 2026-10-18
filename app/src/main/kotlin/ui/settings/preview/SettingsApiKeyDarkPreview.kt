package ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme

@Preview(name = "Settings · API key dark", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SettingsApiKeyDarkPreview() {
    DroidMentorTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(page = SettingsPage.API_KEY, apiKeyConfigured = true),
            isDarkTheme = true, onAction = {}, onBack = {},
        )
    }
}
