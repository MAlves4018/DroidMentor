package ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.theme.DroidMentorTheme

@Preview(name = "Settings · overview light", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SettingsOverviewLightPreview() {
    DroidMentorTheme(darkTheme = false) {
        SettingsScreen(state = SettingsUiState(apiKeyConfigured = true),
            isDarkTheme = false, onAction = {}, onBack = {})
    }
}
