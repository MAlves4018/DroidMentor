package ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import domain.model.Conversation
import domain.model.identity.ConversationId

/**
 * Local host for ActiveChatScreen. FLOW-001 can route a ConversationId here, while later tasks
 * replace local effects without changing the state/action boundary of ActiveChatScreen.
 */
@Composable
fun ActiveChatLocalScreen(
    conversationId: ConversationId,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyText: (String) -> Unit,
    initialConversation: Conversation = Conversation(conversationId),
) {
    val stateHolder = remember(
        conversationId,
        initialConversation,
    ) {
        ActiveChatStateHolder(
            conversationId = conversationId,
            initialConversation = initialConversation,
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
