package ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ui.settings.SettingsScreen
import ui.settings.SettingsUiState
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
                apiKeyDraft = "preview-key-not-real",
            ),
            onAction = {},
            onBack = {},
        )
    }
}
