package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import domain.model.Conversation
import domain.repository.ConversationRepository
import domain.model.identity.ConversationId

/**
 * Local host for ActiveChatScreen. FLOW-001 can route a ConversationId here, while later tasks
 * replace local effects without changing the state/action boundary of ActiveChatScreen.
 */
@Composable
fun ActiveChatLocalScreen(
    conversationId: ConversationId,
    repository: ConversationRepository,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyText: (String) -> Unit,
    initialConversation: Conversation = repository.find(conversationId) ?: Conversation(conversationId),
) {
    val stateHolder = remember(
        conversationId,
    ) {
        ActiveChatStateHolder(
            conversationId = conversationId,
            initialConversation = initialConversation,
            onConversationChanged = repository::save,
        )
    }

    ActiveChatScreen(
        state = stateHolder.state,
        onAction = stateHolder::onAction,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onCopyText = onCopyText,
    )
}
