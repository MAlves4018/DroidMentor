package ui.chat

import androidx.compose.runtime.Composable
import domain.model.identity.ConversationId
import ui.navigation.NavigationAction

@Composable
fun ActiveChatNavigation(
    conversationId: ConversationId,
    onNavigate: (NavigationAction) -> Unit,
    onCopyText: (String) -> Unit
) {
    ActiveChatLocalScreen(
        conversationId = conversationId,

        onOpenHistory = {
            onNavigate(NavigationAction.OpenHistory)
        },

        onOpenSettings = {
            onNavigate(NavigationAction.OpenSettings)
        },

        onCopyText = onCopyText
    )
}
