package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.settings.SettingsScreen
import ui.settings.SettingsUiState
import ui.theme.DroidMentorTheme

@Preview(
    name = "Settings - Configured",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
)
@Composable
fun SettingsConfiguredPreview() {
    DroidMentorTheme(darkTheme = false) {
        SettingsScreen(
            state = SettingsUiState(
                apiKeyDraft = "replacement-preview-key",
                apiKeyConfigured = true,
                apiKeyRevealed = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
