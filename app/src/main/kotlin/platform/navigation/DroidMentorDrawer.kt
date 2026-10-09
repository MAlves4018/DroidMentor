
package platform.navigation

import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.historyChat.HistoryNavigation

@Composable
fun DroidMentorDrawer(
    navigator: DroidMentorNavigator,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    LaunchedEffect(navigator.isHistoryOpen) {
        if (navigator.isHistoryOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    LaunchedEffect(
        drawerState.currentValue,
        drawerState.targetValue
    ) {
        if (
            drawerState.currentValue == DrawerValue.Closed &&
            drawerState.targetValue == DrawerValue.Closed &&
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
                    currentConversationId = navigator.currentConversationId,
                    onNavigate = navigator::handle
                )
            }
        },
        content = content
    )
}
