package ui.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import domain.model.Conversation
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import java.util.UUID
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState

/**
 * Framework-independent Active Chat state machine used before provider effects exist.
 * It owns one immutable Conversation and projects it into the existing UI-002 boundary.
 */
class ActiveChatStateHolder(
    val conversationId: ConversationId,
    initialConversation: Conversation = Conversation(conversationId),
    initialUiState: ActiveChatUiState = ActiveChatUiState(),
    private val onConversationChanged: (Conversation) -> Unit = {},
    private val newMessageId: () -> MessageId = {
        MessageId(UUID.randomUUID().toString())
    },
) {
    init {
        require(initialConversation.id == conversationId) {
            "Initial conversation must match the selected ConversationId."
        }
    }

    private var conversation: Conversation = initialConversation

    // Repository saves are synchronous at this boundary. A save may notify observers
    // immediately; never let an observer re-enter the same state machine before the
    // conversation and draft/edit state have been published.
    private var isPersisting = false

    private val mutableUiState = MutableStateFlow(
        initialUiState.copy(messages = initialConversation.toChatMessagesUi())
    )
    val uiState: StateFlow<ActiveChatUiState> = mutableUiState.asStateFlow()

    /** Synchronous snapshot kept for existing reducer tests and non-Compose callers. */
    var state: ActiveChatUiState
        get() = mutableUiState.value
        private set(value) { mutableUiState.value = value }

    val currentConversation: Conversation
        get() = conversation

    fun onAction(action: ActiveChatAction) {
        if (isPersisting) return

        when (action) {
            is ActiveChatAction.DraftChanged -> {
                state = state.copy(draft = action.value)
            }

            ActiveChatAction.SendClicked -> sendLocalUserMessage()

            is ActiveChatAction.EditMessageClicked -> beginEdit(action.messageId)

            is ActiveChatAction.EditDraftChanged -> updateEditDraft(action.value)

            ActiveChatAction.CancelEditClicked -> cancelEdit()

            ActiveChatAction.SaveEditClicked -> requestRewriteConfirmation()

            ActiveChatAction.ReturnToEditClicked -> returnToEdit()

            ActiveChatAction.ConfirmRewriteClicked -> confirmRewrite()

            ActiveChatAction.RemoveAttachmentClicked -> {
                state = state.copy(attachmentDraft = null)
            }

            is ActiveChatAction.ReasoningSelected -> {
                if (action.level in state.supportedReasoningLevels) {
                    state = state.copy(reasoningLevel = action.level)
                }
            }

            ActiveChatAction.DismissFeedbackClicked -> {
                state = state.copy(requestState = ChatRequestState.Idle)
            }

            ActiveChatAction.StopClicked,
            ActiveChatAction.AddAttachmentClicked,
            ActiveChatAction.RetryClicked,
                -> Unit
        }
    }

    private fun sendLocalUserMessage() {
        val content = state.draft.trim()

        // The state machine protects the same send invariant as the UI. FLOW-002 is text-only;
        // attachment effects are introduced by a later task.
        if (!state.canSend || content.isEmpty()) {
            return
        }

        val message = Message(
            id = newMessageId(),
            role = Role.USER,
            content = content,
            sequence = conversation.nextSequence,
        )

        // Publish the new local snapshot only after the repository has accepted it.
        // If persistence fails, keep the draft and the current conversation intact.
        val updated = conversation.append(message)
        isPersisting = true
        try {
            onConversationChanged(updated)
            conversation = updated
            state = state.copy(
                messages = updated.toChatMessagesUi(),
                draft = "",
            )
        } finally {
            isPersisting = false
        }
    }

    private fun beginEdit(messageId: String) {
        val message = conversation.messages.firstOrNull {
            it.id.value == messageId && it.role == Role.USER
        } ?: return

        if (state.isGenerating) {
            return
        }

        state = state.copy(
            editState = MessageEditState.Editing(
                messageId = message.id.value,
                draft = message.content,
            )
        )
    }

    private fun updateEditDraft(value: String) {
        val editing = state.editState as? MessageEditState.Editing ?: return

        state = state.copy(
            editState = editing.copy(draft = value)
        )
    }

    private fun cancelEdit() {
        if (state.editState !is MessageEditState.Editing) {
            return
        }

        state = state.copy(editState = MessageEditState.None)
    }

    private fun requestRewriteConfirmation() {
        val editing = state.editState as? MessageEditState.Editing ?: return
        val replacement = editing.draft.trim()

        if (replacement.isEmpty()) {
            return
        }

        val current = conversation.messages.firstOrNull {
            it.id.value == editing.messageId && it.role == Role.USER
        } ?: return

        if (replacement == current.content) {
            state = state.copy(editState = MessageEditState.None)
            return
        }

        state = state.copy(
            editState = MessageEditState.ConfirmRewrite(
                messageId = editing.messageId,
                replacementText = replacement,
            )
        )
    }

    private fun returnToEdit() {
        val confirmation = state.editState as? MessageEditState.ConfirmRewrite ?: return

        state = state.copy(
            editState = MessageEditState.Editing(
                messageId = confirmation.messageId,
                draft = confirmation.replacementText,
            )
        )
    }

    private fun confirmRewrite() {
        if (state.isGenerating) return
        val confirmation = state.editState as? MessageEditState.ConfirmRewrite ?: return

        val target = conversation.messages.firstOrNull {
            it.id.value == confirmation.messageId && it.role == Role.USER
        } ?: return

        // Do not drop the original response suffix until the replacement is stored.
        // A failed write leaves the confirmation available for an explicit retry.
        val updated = conversation.rewriteUserMessage(
            messageId = target.id,
            replacementText = confirmation.replacementText,
        )
        isPersisting = true
        try {
            onConversationChanged(updated)
            conversation = updated
            state = state.copy(
                messages = updated.toChatMessagesUi(),
                editState = MessageEditState.None,
                requestState = ChatRequestState.Idle,
            )
        } finally {
            isPersisting = false
        }
    }
}
