package ui.settings

sealed interface SettingsAction {
    data object ToggleThemeClicked : SettingsAction
    data object OpenApiKeyClicked : SettingsAction
    data object CloseApiKeyClicked : SettingsAction
    data object OpenAboutClicked : SettingsAction
    data class ApiKeyChanged(val value: String) : SettingsAction
    data object ToggleApiKeyVisibility : SettingsAction
    data object SaveKeyClicked : SettingsAction
    data object RequestRemoveKeyClicked : SettingsAction
    data object CancelRemoveKeyClicked : SettingsAction
    data object RemoveKeyClicked : SettingsAction
}
