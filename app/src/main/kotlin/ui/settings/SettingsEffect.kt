package ui.settings

/** One-off commands executed by the Settings navigation host. Storage stays in the ViewModel. */
sealed interface SettingsEffect {
    data object ToggleTheme : SettingsEffect
    data object OpenAbout : SettingsEffect
}
