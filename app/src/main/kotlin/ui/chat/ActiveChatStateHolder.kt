package ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import domain.model.Conversation
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import java.util.UUID
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState

/**
 * Local Active Chat state machine used before provider and persistence effects exist.
 * It owns one immutable Conversation and projects it into the existing UI-002 boundary.
 */
class ActiveChatStateHolder(
    val conversationId: ConversationId,
    initialConversation: Conversation = Conversation(conversationId),
    initialUiState: ActiveChatUiState = ActiveChatUiState(),
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

    var state: ActiveChatUiState by mutableStateOf(
        initialUiState.copy(
            messages = initialConversation.toChatMessagesUi(),
        )
    )
        private set

    val currentConversation: Conversation
        get() = conversation

    fun onAction(action: ActiveChatAction) {
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

        conversation = conversation.append(message)

        state = state.copy(
            messages = conversation.toChatMessagesUi(),
            draft = "",
        )
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
        val confirmation = state.editState as? MessageEditState.ConfirmRewrite ?: return

        val target = conversation.messages.firstOrNull {
            it.id.value == confirmation.messageId && it.role == Role.USER
        } ?: return

        conversation = conversation.rewriteUserMessage(
            messageId = target.id,
            replacementText = confirmation.replacementText,
        )

        state = state.copy(
            messages = conversation.toChatMessagesUi(),
            editState = MessageEditState.None,
            requestState = ChatRequestState.Idle,
        )
    }
}
