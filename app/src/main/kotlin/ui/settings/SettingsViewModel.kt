package ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns the Settings editor and coordinates durable BYOK operations via its repository. */
class SettingsViewModel(
    private val repository: SettingsRepository,
    initialState: SettingsUiState = SettingsUiState(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState.copy(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.hasApiKey
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableUiState.update {
                        it.copy(isLoading = false, errorMessage = "Could not read API key settings.")
                    }
                }
                .collect { configured ->
                    mutableUiState.update {
                        it.copy(apiKeyConfigured = configured, isLoading = false)
                    }
                }
        }
    }

    fun onAction(action: SettingsAction): SettingsEffect? = when (action) {
        SettingsAction.ToggleThemeClicked -> SettingsEffect.ToggleTheme
        SettingsAction.OpenAboutClicked -> SettingsEffect.OpenAbout
        SettingsAction.OpenApiKeyClicked -> {
            mutableUiState.update { it.copy(page = SettingsPage.API_KEY, errorMessage = null) }
            null
        }
        SettingsAction.CloseApiKeyClicked -> {
            mutableUiState.update {
                it.copy(page = SettingsPage.OVERVIEW, apiKeyRevealed = false,
                    confirmRemoveKey = false, errorMessage = null)
            }
            null
        }
        is SettingsAction.ApiKeyChanged -> {
            if (!mutableUiState.value.isSaving) {
                mutableUiState.update { it.copy(apiKeyDraft = action.value, errorMessage = null) }
            }
            null
        }
        SettingsAction.ToggleApiKeyVisibility -> {
            mutableUiState.update { it.copy(apiKeyRevealed = !it.apiKeyRevealed) }
            null
        }
        SettingsAction.SaveKeyClicked -> {
            val current = mutableUiState.value
            if (current.canSave) {
                val draft = current.apiKeyDraft.trim()
                mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
                viewModelScope.launch {
                    try {
                        repository.saveApiKey(draft)
                        mutableUiState.update {
                            it.copy(apiKeyDraft = "", apiKeyRevealed = false, isSaving = false)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // Do not log the key or exception: only a constant, safe UI message.
                        mutableUiState.update {
                            it.copy(isSaving = false, errorMessage = "Could not save API key. Try again.")
                        }
                    }
                }
            }
            null
        }
        SettingsAction.RequestRemoveKeyClicked -> {
            mutableUiState.update {
                if (it.apiKeyConfigured && !it.isLoading && !it.isSaving) {
                    it.copy(confirmRemoveKey = true, errorMessage = null)
                } else it
            }
            null
        }
        SettingsAction.CancelRemoveKeyClicked -> {
            mutableUiState.update {
                if (it.isSaving) it else it.copy(confirmRemoveKey = false)
            }
            null
        }
        SettingsAction.RemoveKeyClicked -> {
            val current = mutableUiState.value
            if (current.confirmRemoveKey && current.apiKeyConfigured && !current.isSaving) {
                mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
                viewModelScope.launch {
                    try {
                        repository.removeApiKey()
                        mutableUiState.update {
                            it.copy(confirmRemoveKey = false, apiKeyDraft = "",
                                apiKeyRevealed = false, isSaving = false)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        mutableUiState.update {
                            it.copy(isSaving = false, errorMessage = "Could not remove API key. Try again.")
                        }
                    }
                }
            }
            null
        }
    }
}
