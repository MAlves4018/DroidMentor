package ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps local Settings navigation and the API-key draft independently testable. */
class SettingsViewModel(initialState: SettingsUiState = SettingsUiState()) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    fun onAction(action: SettingsAction): SettingsEffect? = when (action) {
        SettingsAction.ToggleThemeClicked -> SettingsEffect.ToggleTheme
        SettingsAction.OpenAboutClicked -> SettingsEffect.OpenAbout
        SettingsAction.OpenApiKeyClicked -> {
            mutableUiState.value = mutableUiState.value.copy(page = SettingsPage.API_KEY)
            null
        }
        SettingsAction.CloseApiKeyClicked -> {
            mutableUiState.value = mutableUiState.value.copy(
                page = SettingsPage.OVERVIEW,
                apiKeyRevealed = false,
                confirmRemoveKey = false,
            )
            null
        }
        is SettingsAction.ApiKeyChanged -> {
            mutableUiState.value = mutableUiState.value.copy(apiKeyDraft = action.value)
            null
        }
        SettingsAction.ToggleApiKeyVisibility -> {
            val current = mutableUiState.value
            mutableUiState.value = current.copy(apiKeyRevealed = !current.apiKeyRevealed)
            null
        }
        SettingsAction.SaveKeyClicked ->
            mutableUiState.value.apiKeyDraft.takeIf(String::isNotBlank)?.let(SettingsEffect::SaveKey)
        SettingsAction.RequestRemoveKeyClicked -> {
            if (mutableUiState.value.apiKeyConfigured) {
                mutableUiState.value = mutableUiState.value.copy(confirmRemoveKey = true)
            }
            null
        }
        SettingsAction.CancelRemoveKeyClicked -> {
            mutableUiState.value = mutableUiState.value.copy(confirmRemoveKey = false)
            null
        }
        SettingsAction.RemoveKeyClicked -> {
            if (!mutableUiState.value.apiKeyConfigured || !mutableUiState.value.confirmRemoveKey) null
            else {
                mutableUiState.value = mutableUiState.value.copy(confirmRemoveKey = false)
                SettingsEffect.RemoveKey
            }
        }
    }
}
