package ui.title

import androidx.compose.runtime.Composable
import ui.navigation.NavigationAction

@Composable
fun TitleNavigation(
    onNavigate: (NavigationAction) -> Unit
) {
    TitleScreen(
        onNewChat = {
            onNavigate(NavigationAction.NewChat)
        },
        onOpenHistory = {
            onNavigate(NavigationAction.OpenHistory)
        },
        onOpenSettings = {
            onNavigate(NavigationAction.OpenSettings)
        },
        onOpenAbout = {
            onNavigate(NavigationAction.OpenAbout)
        }
    )
}
