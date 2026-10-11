package ui.chat

import androidx.compose.runtime.Composable
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import ui.navigation.NavigationAction

@Composable
fun ActiveChatNavigation(
    conversationId: ConversationId,
    repository: ConversationRepository,
    onNavigate: (NavigationAction) -> Unit,
    onCopyText: (String) -> Unit
) {
    ActiveChatLocalScreen(
        conversationId = conversationId,
        repository = repository,

        onOpenHistory = {
            onNavigate(NavigationAction.OpenHistory)
        },

        onOpenSettings = {
            onNavigate(NavigationAction.OpenSettings)
        },

        onCopyText = onCopyText
    )
}
