package ui.about

import androidx.compose.runtime.Composable
import ui.navigation.NavigationAction

@Composable
fun AboutNavigation(
    onNavigate: (NavigationAction) -> Unit
) {
    AboutScreen(
        onBack = {
            onNavigate(NavigationAction.Back)
        }
    )
}
