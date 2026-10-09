package ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ui.chat.components.composer.ChatComposer
import ui.chat.components.content.ActiveChatContent
import ui.chat.model.MessageEditState
import ui.common.ConfirmDialog
import ui.common.DroidMentorTopBar

/**
 * Pure Active Chat surface: immutable state in, typed chat intents out.
 * Routing, clipboard work, file picking, provider calls and persistence remain outside this composable.
 */
@Composable
fun ActiveChatScreen(
    state: ActiveChatUiState,
    onAction: (ActiveChatAction) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.editState is MessageEditState.Editing) {
        onAction(ActiveChatAction.CancelEditClicked)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DroidMentorTopBar(
                title = state.title,
                onMenu = onOpenHistory,
            )
        },
        bottomBar = {
            ChatComposer(
                draft = state.draft,
                onDraftChange = { onAction(ActiveChatAction.DraftChanged(it)) },
                canSend = state.canSend,
                isGenerating = state.isGenerating,
                attachmentsEnabled = state.attachmentsEnabled,
                attachment = state.attachmentDraft,
                onAttachClick = { onAction(ActiveChatAction.AddAttachmentClicked) },
                onRemoveAttachment = { onAction(ActiveChatAction.RemoveAttachmentClicked) },
                supportedReasoningLevels = state.supportedReasoningLevels,
                reasoningLevel = state.reasoningLevel,
                onReasoningSelected = { onAction(ActiveChatAction.ReasoningSelected(it)) },
                onSend = { onAction(ActiveChatAction.SendClicked) },
                onStop = { onAction(ActiveChatAction.StopClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        ActiveChatContent(
            state = state,
            onAction = onAction,
            onCopyText = onCopyText,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }

    if (state.editState is MessageEditState.ConfirmRewrite) {
        ConfirmDialog(
            title = "Rewrite conversation?",
            message = "Editing this message will replace the messages that follow it.",
            confirmLabel = "Continue",
            onConfirm = { onAction(ActiveChatAction.ConfirmRewriteClicked) },
            onCancel = { onAction(ActiveChatAction.ReturnToEditClicked) },
            modifier = Modifier.testTag(ActiveChatTestTags.CONFIRM_REWRITE),
            destructive = true,
        )
    }
}
