package ui.chat.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ui.chat.ActiveChatAction
import ui.chat.ActiveChatTestTags
import ui.chat.ActiveChatUiState
import ui.chat.components.feedback.ThinkingIndicator
import ui.chat.components.message.EditMessageView
import ui.chat.components.message.MessageBubble
import ui.chat.model.ChatMessageRole
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState

/** Scrollable conversation matching CHAT-01/13, with a jump affordance when older turns are visible. */
@Composable
fun MessageList(
    state: ActiveChatUiState,
    onAction: (ActiveChatAction) -> Unit,
        onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val hasOlderContent by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) < total - 1
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag(ActiveChatTestTags.MESSAGE_LIST),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = state.messages, key = { it.id }) { message ->
                val editState = state.editState
                if (message.role == ChatMessageRole.USER &&
                    editState is MessageEditState.Editing && editState.messageId == message.id
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
                item { ThinkingIndicator() }
            }
        }
        if (hasOlderContent) {
            FilledTonalIconButton(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 10.dp)
                    .testTag(ActiveChatTestTags.JUMP_LATEST),
                onClick = {
                    scope.launch {
                        val last = listState.layoutInfo.totalItemsCount - 1
                        if (last >= 0) listState.animateScrollToItem(last)
                    }
                },
            ) {
                Icon(imageVector = Icons.Filled.ArrowDownward, contentDescription = "Jump to latest")
            }
        }
    }
}
