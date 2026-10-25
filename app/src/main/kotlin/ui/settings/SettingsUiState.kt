package ui.settings

/** UI never exposes the stored key. Only the unsaved editor draft may contain its value. */
data class SettingsUiState(
    val page: SettingsPage = SettingsPage.OVERVIEW,
    val apiKeyDraft: String = "",
    val apiKeyConfigured: Boolean = false,
    val apiKeyRevealed: Boolean = false,
    val confirmRemoveKey: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean get() = !isLoading && !isSaving && apiKeyDraft.isNotBlank()
}
