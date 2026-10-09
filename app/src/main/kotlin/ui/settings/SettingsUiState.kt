package ui.settings

/** Presentation state for the Settings surface. Persistence is introduced later. */
data class SettingsUiState(
    val apiKeyDraft: String = "",
    val apiKeyConfigured: Boolean = false,
    val apiKeyRevealed: Boolean = false,
) {
    val canSave: Boolean
        get() = apiKeyDraft.isNotBlank()
}
