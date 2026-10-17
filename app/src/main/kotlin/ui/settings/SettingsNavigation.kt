package ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ui.navigation.NavigationAction

/** Host for Settings and its API-key subpage. Theme belongs to the application root. */
@Composable
fun SettingsNavigation(
    onNavigate: (NavigationAction) -> Unit,
    isDarkTheme: Boolean = false,
    onThemeChange: () -> Unit = {},
    onSaveKey: (String) -> Unit = {},
    onRemoveKey: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goBack: () -> Unit = {
        when {
            state.confirmRemoveKey -> viewModel.onAction(SettingsAction.CancelRemoveKeyClicked)
            state.page == SettingsPage.API_KEY -> viewModel.onAction(SettingsAction.CloseApiKeyClicked)
            else -> onNavigate(NavigationAction.Back)
        }
    }
    BackHandler(enabled = state.page == SettingsPage.API_KEY || state.confirmRemoveKey) {
        goBack()
    }
    SettingsScreen(
        state = state,
        isDarkTheme = isDarkTheme,
        onAction = { action ->
            when (val effect = viewModel.onAction(action)) {
                SettingsEffect.ToggleTheme -> onThemeChange()
                SettingsEffect.OpenAbout -> onNavigate(NavigationAction.OpenAbout)
                is SettingsEffect.SaveKey -> onSaveKey(effect.value)
                SettingsEffect.RemoveKey -> onRemoveKey()
                null -> Unit
            }
        },
        onBack = goBack,
    )
}
