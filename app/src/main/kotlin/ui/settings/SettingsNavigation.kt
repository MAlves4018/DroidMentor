
package ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import isel.dei.pdm.droidmentor.DroidMentorApplication
import kotlinx.coroutines.launch
import ui.navigation.NavigationAction
import androidx.compose.runtime.LaunchedEffect

/** Host for Settings and its API-key subpage. Theme belongs to the application root. */
@Composable
fun SettingsNavigation(
    onNavigate: (NavigationAction) -> Unit,
    isDarkTheme: Boolean = false,
    onThemeChange: () -> Unit = {},
) {
    // Get the repository from the Application service locator
    val application = LocalContext.current.applicationContext
            as DroidMentorApplication

    val repository = application.settings
    val scope = rememberCoroutineScope()

    // Existing ViewModel
    val viewModel: SettingsViewModel = viewModel()
    val localState by viewModel.uiState.collectAsStateWithLifecycle()

    // Observe whether the repository has an API key
    val hasApiKey by repository.hasApiKey.collectAsStateWithLifecycle(
        initialValue = false
    )

    // Synchronize the ViewModel with the repository
    LaunchedEffect(hasApiKey) {
        viewModel.updateApiKeyConfigured(hasApiKey)
    }

    // Use the repository's value as the source of truth
    val state = localState.copy(apiKeyConfigured = hasApiKey)

    val goBack: () -> Unit = {
        when {
            state.confirmRemoveKey ->
                viewModel.onAction(SettingsAction.CancelRemoveKeyClicked)

            state.page == SettingsPage.API_KEY ->
                viewModel.onAction(SettingsAction.CloseApiKeyClicked)

            else -> onNavigate(NavigationAction.Back)
        }
    }

    BackHandler(
        enabled = state.page == SettingsPage.API_KEY || state.confirmRemoveKey
    ) {
        goBack()
    }

    SettingsScreen(
        state = state,
        isDarkTheme = isDarkTheme,
        onAction = { action ->
            when (val effect = viewModel.onAction(action)) {

                SettingsEffect.ToggleTheme ->
                    onThemeChange()

                SettingsEffect.OpenAbout ->
                    onNavigate(NavigationAction.OpenAbout)

                is SettingsEffect.SaveKey -> {
                    scope.launch {
                        repository.saveApiKey(effect.value)
                    }
                }

                SettingsEffect.RemoveKey -> {
                    scope.launch {
                        repository.removeApiKey()
                    }
                }

                null -> Unit
            }
        },
        onBack = goBack,
    )
}
