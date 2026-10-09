package ui.settings

import androidx.compose.runtime.*
import ui.navigation.NavigationAction

@Composable
fun SettingsNavigation(
    onNavigate: (NavigationAction) -> Unit,
    onSaveKey: (String) -> Unit = {},
    onRemoveKey: () -> Unit = {}
) {
    var state by remember {
        mutableStateOf(SettingsUiState())
    }

    SettingsScreen(
        state = state,

        onAction = { action ->
            when (action) {

                is SettingsAction.ApiKeyChanged -> {
                    state = state.copy(
                        apiKeyDraft = action.value
                    )
                }

                SettingsAction.ToggleApiKeyVisibility -> {
                    state = state.copy(
                        apiKeyRevealed = !state.apiKeyRevealed
                    )
                }

                SettingsAction.SaveKeyClicked -> {
                    onSaveKey(state.apiKeyDraft)
                }

                SettingsAction.RemoveKeyClicked -> {
                    onRemoveKey()
                }
            }
        },

        onBack = {
            onNavigate(NavigationAction.Back)
        }
    )
}
