package ui.chat

import ui.chat.model.ChatAttachmentUi
import ui.chat.model.ChatMessageUi
import ui.chat.model.ChatRequestState
import ui.chat.model.MessageEditState
import ui.chat.model.ReasoningLevelUi

/**
 * Immutable snapshot required to render Active Chat.
 *
 * W1 intentionally keeps this independent from provider/database code and the W2 state holder so
 * previews and Compose tests can reproduce every important visual state directly.
 */
data class ActiveChatUiState(
    val title: String = "DroidMentor",
    val messages: List<ChatMessageUi> = emptyList(),
    val draft: String = "",
    val attachmentDraft: ChatAttachmentUi? = null,
    val attachmentsEnabled: Boolean = true,
    val requestState: ChatRequestState = ChatRequestState.Idle,
    val editState: MessageEditState = MessageEditState.None,
    val isOnline: Boolean = true,
    val providerConfigured: Boolean = true,
    val reasoningLevel: ReasoningLevelUi = ReasoningLevelUi.AUTO,
    val supportedReasoningLevels: Set<ReasoningLevelUi> = emptySet(),
) {
    val isGenerating: Boolean
        get() = requestState is ChatRequestState.Generating

    val canSend: Boolean
        get() =
            (draft.isNotBlank() || attachmentDraft != null) &&
                isOnline &&
                providerConfigured &&
                !isGenerating &&
                editState is MessageEditState.None
}
