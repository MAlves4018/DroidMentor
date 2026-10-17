package ui.settings

/** UI state only. The real configured flag is supplied by NET-002/SettingsRepository. */
data class SettingsUiState(
    val page: SettingsPage = SettingsPage.OVERVIEW,
    val apiKeyDraft: String = "",
    val apiKeyConfigured: Boolean = false,
    val apiKeyRevealed: Boolean = false,
    val confirmRemoveKey: Boolean = false,
) {
    val canSave: Boolean get() = apiKeyDraft.isNotBlank()
}
