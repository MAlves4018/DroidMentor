package ui.chat

import androidx.lifecycle.ViewModel
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import domain.model.identity.MessageId
import java.util.UUID
import kotlinx.coroutines.flow.StateFlow

/**
 * Lifecycle-aware presentation owner for one conversation.
 * The repository owns conversation data; the state holder owns local UI transitions.
 */
class ActiveChatViewModel(
    val conversationId: ConversationId,
    repository: ConversationRepository,
    initialUiState: ActiveChatUiState = ActiveChatUiState(),
    newMessageId: () -> MessageId = { MessageId(UUID.randomUUID().toString()) },
) : ViewModel() {
    private val stateHolder = ActiveChatStateHolder(
        conversationId = conversationId,
        initialConversation = requireNotNull(repository.find(conversationId)) {
            "Cannot open unknown or deleted conversation: ${conversationId.value}"
        },
        initialUiState = initialUiState,
        onConversationChanged = repository::save,
        newMessageId = newMessageId,
    )

    val uiState: StateFlow<ActiveChatUiState> = stateHolder.uiState

    fun onAction(action: ActiveChatAction) {
        stateHolder.onAction(action)
    }
}
