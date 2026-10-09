package ui.chat.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatAction
import ui.chat.ActiveChatTestTags
import ui.chat.ActiveChatUiState
import ui.chat.components.feedback.ThinkingIndicator
import ui.chat.components.message.EditMessageView
import ui.chat.components.message.MessageBubble
import ui.chat.model.ChatMessageRole
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState

/** Ordered conversation body. Scroll conveniences can be added later without changing the screen contract. */
@Composable
fun MessageList(
    state: ActiveChatUiState,
    onAction: (ActiveChatAction) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(ActiveChatTestTags.MESSAGE_LIST),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = state.messages,
            key = { it.id },
        ) { message ->
            val editState = state.editState

            if (
                message.role == ChatMessageRole.USER &&
                editState is MessageEditState.Editing &&
                editState.messageId == message.id
            ) {
                EditMessageView(
                    draft = editState.draft,
                    onDraftChange = { onAction(ActiveChatAction.EditDraftChanged(it)) },
                    onCancel = { onAction(ActiveChatAction.CancelEditClicked) },
                    onSave = { onAction(ActiveChatAction.SaveEditClicked) },
                )
            } else {
                MessageBubble(
                    message = message,
                    canEdit = message.canEdit && !state.isGenerating,
                    onCopyText = onCopyText,
                    onEdit = { onAction(ActiveChatAction.EditMessageClicked(message.id)) },
                )
            }
        }

        if (state.requestState is ChatRequestState.Generating) {
            item {
                ThinkingIndicator()
            }
        }
    }
}
