package ui.chat

import androidx.compose.runtime.Composable
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import androidx.lifecycle.ViewModelStoreOwner
import ui.navigation.NavigationAction

@Composable
fun ActiveChatNavigation(
    conversationId: ConversationId,
    repository: ConversationRepository,
    viewModelStoreOwner: ViewModelStoreOwner,
    onNavigate: (NavigationAction) -> Unit,
    onCopyText: (String) -> Unit
) {
    ActiveChatLocalScreen(
        conversationId = conversationId,
        repository = repository,
        viewModelStoreOwner = viewModelStoreOwner,

        onOpenHistory = {
            onNavigate(NavigationAction.OpenHistory)
        },

        onOpenSettings = {
            onNavigate(NavigationAction.OpenSettings)
        },

        onCopyText = onCopyText
    )
}
