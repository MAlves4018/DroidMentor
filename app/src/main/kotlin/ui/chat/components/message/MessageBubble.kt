package ui.chat.components.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.chat.model.ChatMessageRole
import ui.chat.model.ChatMessageUi

/** Dispatches a chat turn to its role-specific visual representation. */
@Composable
fun MessageBubble(
    message: ChatMessageUi,
    canEdit: Boolean,
    onCopyText: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (message.role) {
        ChatMessageRole.USER -> UserMessageContent(
            message = message,
            canEdit = canEdit,
            onCopy = { onCopyText(message.content) },
            onEdit = onEdit,
            modifier = modifier,
        )

        ChatMessageRole.MODEL -> ModelMessageContent(
            message = message,
            onCopyText = onCopyText,
            modifier = modifier,
        )
    }
}
