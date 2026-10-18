package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelStoreOwner
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository

/** Host only: collects ViewModel state and forwards typed UI actions. */
@Composable
fun ActiveChatLocalScreen(
    conversationId: ConversationId,
    repository: ConversationRepository,
    viewModelStoreOwner: ViewModelStoreOwner,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyText: (String) -> Unit,
) {
    // One owner per conversation. Unlike Activity-scoped keys, deleted chats can be cleared.
    val viewModel: ActiveChatViewModel = viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = ActiveChatViewModelFactory(conversationId, repository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ActiveChatScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onCopyText = onCopyText,
    )
}
