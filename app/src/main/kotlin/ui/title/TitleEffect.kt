package ui.title

import ui.navigation.NavigationAction

/** Single-use command delivered to the host, not retained in screen state. */
sealed interface TitleEffect {
    data class Navigate(val action: NavigationAction) : TitleEffect
}
