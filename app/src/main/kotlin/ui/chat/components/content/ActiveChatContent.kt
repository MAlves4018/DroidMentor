package ui.chat.components.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.chat.ActiveChatAction
import ui.chat.ActiveChatUiState
import ui.chat.components.feedback.RequestFeedback

/** Chooses the empty/conversation body while keeping request feedback in one stable region. */
@Composable
fun ActiveChatContent(
    state: ActiveChatUiState,
    onAction: (ActiveChatAction) -> Unit,
    onCopyText: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        RequestFeedback(
            state = state,
            onRetry = { onAction(ActiveChatAction.RetryClicked) },
            onDismiss = { onAction(ActiveChatAction.DismissFeedbackClicked) },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )

        if (state.messages.isEmpty()) {
            EmptyChatContent(modifier = Modifier.weight(1f))
        } else {
            MessageList(
                state = state,
                onAction = onAction,
                onCopyText = onCopyText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
