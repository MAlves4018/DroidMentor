package ui.settings

/** Synchronous host commands. NET-002 supplies durable key persistence later. */
sealed interface SettingsEffect {
    data class SaveKey(val value: String) : SettingsEffect
    data object RemoveKey : SettingsEffect
    data object ToggleTheme : SettingsEffect
    data object OpenAbout : SettingsEffect
}
