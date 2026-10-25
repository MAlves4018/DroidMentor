package ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import isel.dei.pdm.droidmentor.DroidMentorApplication
import ui.navigation.NavigationAction

/** Gets the lifecycle-aware presentation controller; does not execute storage operations. */
@Composable
fun SettingsNavigation(
    onNavigate: (NavigationAction) -> Unit,
    isDarkTheme: Boolean = false,
    onThemeChange: () -> Unit = {},
) {
    val application = LocalContext.current.applicationContext as DroidMentorApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application.settings),
    )
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
            when (viewModel.onAction(action)) {
                SettingsEffect.ToggleTheme -> onThemeChange()
                SettingsEffect.OpenAbout -> onNavigate(NavigationAction.OpenAbout)
                null -> Unit
            }
        },
        onBack = goBack,
    )
}
