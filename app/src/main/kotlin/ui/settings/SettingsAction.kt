package ui.settings

/** User intents emitted by Settings. Effects are handled by later integration tasks. */
sealed interface SettingsAction {
    data class ApiKeyChanged(val value: String) : SettingsAction
    data object ToggleApiKeyVisibility : SettingsAction
    data object SaveKeyClicked : SettingsAction
    data object RemoveKeyClicked : SettingsAction
}
