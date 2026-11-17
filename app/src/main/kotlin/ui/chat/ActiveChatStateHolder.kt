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
 * Framework-independent Active Chat state machine.
 * Owns one immutable Conversation and projects it into the UI state.
 */
class ActiveChatStateHolder(
    val conversationId: ConversationId,
    initialConversation: Conversation = Conversation(conversationId),
    initialUiState: ActiveChatUiState = ActiveChatUiState(),
    private val onConversationChanged: suspend (Conversation) -> Unit = {},
    private val newMessageId: () -> MessageId = {
        MessageId(UUID.randomUUID().toString())
    },
    private val onRewriteRequested:
    (suspend (MessageId, String) -> Conversation)? = null,
) {
    init {
        require(initialConversation.id == conversationId) {
            "Initial conversation must match the selected ConversationId."
        }
    }

    private var conversation: Conversation = initialConversation

    private var isPersisting = false
    private var draftRevision = 0L

    private val mutableUiState = MutableStateFlow(
        initialUiState.copy(messages = initialConversation.toChatMessagesUi())
    )

    val uiState: StateFlow<ActiveChatUiState> = mutableUiState.asStateFlow()

    var state: ActiveChatUiState
        get() = mutableUiState.value
        private set(value) {
            mutableUiState.value = value
        }

    val currentConversation: Conversation
        get() = conversation

    fun setProviderConfigured(configured: Boolean) {
        state = state.copy(providerConfigured = configured)
    }

    fun setRequestState(requestState: ChatRequestState) {
        state = state.copy(requestState = requestState)
    }

    suspend fun appendModelReply(text: String): Boolean {
        val content = text.trim()

        if (
            isPersisting ||
            state.requestState !is ChatRequestState.Generating ||
            content.isEmpty()
        ) {
            return false
        }

        val message = Message(
            id = newMessageId(),
            role = Role.MODEL,
            content = content,
            sequence = conversation.nextSequence,
        )

        val updated = conversation.append(message)

        isPersisting = true

        try {
            onConversationChanged(updated)

            conversation = updated

            state = state.copy(
                messages = updated.toChatMessagesUi()
            )

            return true
        } finally {
            isPersisting = false
        }
    }

    suspend fun onAction(action: ActiveChatAction) {
        if (
            isPersisting &&
            action !is ActiveChatAction.DraftChanged
        ) {
            return
        }

        when (action) {
            is ActiveChatAction.DraftChanged -> {
                draftRevision++

                state = state.copy(
                    draft = action.value
                )
            }

            ActiveChatAction.SendClicked -> sendLocalUserMessage()

            is ActiveChatAction.EditMessageClicked ->
                beginEdit(action.messageId)

            is ActiveChatAction.EditDraftChanged ->
                updateEditDraft(action.value)

            ActiveChatAction.CancelEditClicked ->
                cancelEdit()

            ActiveChatAction.SaveEditClicked ->
                requestRewriteConfirmation()

            ActiveChatAction.ReturnToEditClicked ->
                returnToEdit()

            ActiveChatAction.ConfirmRewriteClicked ->
                confirmRewrite()

            ActiveChatAction.RemoveAttachmentClicked -> {
                state = state.copy(
                    attachmentDraft = null
                )
            }

            is ActiveChatAction.ReasoningSelected -> {
                if (action.level in state.supportedReasoningLevels) {
                    state = state.copy(
                        reasoningLevel = action.level
                    )
                }
            }

            ActiveChatAction.DismissFeedbackClicked -> {
                state = state.copy(
                    requestState = ChatRequestState.Idle
                )
            }

            ActiveChatAction.StopClicked,
            ActiveChatAction.AddAttachmentClicked,
            ActiveChatAction.RetryClicked -> Unit
        }
    }

    private suspend fun sendLocalUserMessage() {
        val submittedDraft = state.draft
        val submittedRevision = draftRevision
        val content = submittedDraft.trim()

        if (!state.canSend || content.isEmpty()) {
            return
        }

        val message = Message(
            id = newMessageId(),
            role = Role.USER,
            content = content,
            sequence = conversation.nextSequence,
        )

        val updated = conversation.append(message)

        isPersisting = true

        try {
            onConversationChanged(updated)

            conversation = updated

            state = state.copy(
                messages = updated.toChatMessagesUi(),
                draft = if (draftRevision == submittedRevision) {
                    ""
                } else {
                    state.draft
                },
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
        val editing =
            state.editState as? MessageEditState.Editing ?: return

        state = state.copy(
            editState = editing.copy(
                draft = value
            )
        )
    }

    private fun cancelEdit() {
        if (state.editState !is MessageEditState.Editing) {
            return
        }

        state = state.copy(
            editState = MessageEditState.None
        )
    }

    private fun requestRewriteConfirmation() {
        val editing =
            state.editState as? MessageEditState.Editing ?: return

        val replacement = editing.draft.trim()

        if (replacement.isEmpty()) {
            return
        }

        val current = conversation.messages.firstOrNull {
            it.id.value == editing.messageId &&
                    it.role == Role.USER
        } ?: return

        if (replacement == current.content) {
            state = state.copy(
                editState = MessageEditState.None
            )
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
        val confirmation =
            state.editState as? MessageEditState.ConfirmRewrite
                ?: return

        state = state.copy(
            editState = MessageEditState.Editing(
                messageId = confirmation.messageId,
                draft = confirmation.replacementText,
            )
        )
    }

    private suspend fun confirmRewrite() {
        if (state.isGenerating) return

        val confirmation =
            state.editState as? MessageEditState.ConfirmRewrite
                ?: return

        isPersisting = true

        try {
            val messageId = MessageId(confirmation.messageId)

            val updated = if (onRewriteRequested != null) {
                // Production: rewrite directly from persisted data.
                onRewriteRequested.invoke(
                    messageId,
                    confirmation.replacementText
                )
            } else {
                // Compatibility for existing in-memory state-holder tests.
                val result = conversation.rewriteUserMessage(
                    messageId = messageId,
                    replacementText = confirmation.replacementText,
                )

                onConversationChanged(result)
                result
            }

            check(updated.id == conversationId) {
                "Rewritten conversation ID does not match."
            }

            // Publish the returned persisted conversation.
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
