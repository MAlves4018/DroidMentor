
package platform.navigation

import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import ui.historyChat.HistoryNavigation
import domain.repository.ConversationRepository
import domain.model.identity.ConversationId
import ui.navigation.NavigationAction

@Composable
fun DroidMentorDrawer(
    navigator: DroidMentorNavigator,
    repository: ConversationRepository,
    onConversationDeleted: (ConversationId) -> Unit,
    onNavigate: (NavigationAction) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(navigator.isHistoryOpen) {
        if (navigator.isHistoryOpen) {
            // The composer must not keep the IME over the drawer's actions.
            // Clearing focus leaves the ViewModel-owned draft intact.
            focusManager.clearFocus()
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        if (
            drawerState.currentValue == DrawerValue.Closed &&
            navigator.isHistoryOpen
        ) {
            navigator.closeHistory()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = navigator.isHistoryOpen ||
                navigator.currentDestination is DroidMentorDestination.ActiveChat,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(240.dp)
            ) {
                HistoryNavigation(
                    repository = repository,
                    onConversationDeleted = onConversationDeleted,
                    onNavigate = onNavigate
                )
            }
        },
        content = content
    )
}
