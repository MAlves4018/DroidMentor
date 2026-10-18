package ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.common.ConfirmDialog
import ui.common.DroidMentorTopBar

/** Visual dispatcher: overview and API key are distinct mockup surfaces. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DroidMentorTopBar(
                title = if (state.page == SettingsPage.API_KEY) "API key" else "Settings",
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (state.page) {
            SettingsPage.OVERVIEW -> SettingsOverviewScreen(
                isDarkTheme = isDarkTheme,
                apiKeyConfigured = state.apiKeyConfigured,
                onThemeToggle = { onAction(SettingsAction.ToggleThemeClicked) },
                onOpenApiKey = { onAction(SettingsAction.OpenApiKeyClicked) },
                onOpenAbout = { onAction(SettingsAction.OpenAboutClicked) },
                contentPadding = padding,
            )
            SettingsPage.API_KEY -> SettingsApiKeyScreen(
                state = state,
                onAction = onAction,
                contentPadding = padding,
            )
        }
    }
    if (state.confirmRemoveKey) {
        ConfirmDialog(
            title = "Remove API key?",
            message = "The configured key will be removed from this device.",
            confirmLabel = "Remove",
            onConfirm = { onAction(SettingsAction.RemoveKeyClicked) },
            onCancel = { onAction(SettingsAction.CancelRemoveKeyClicked) },
            destructive = true,
            modifier = Modifier,
        )
    }
}
