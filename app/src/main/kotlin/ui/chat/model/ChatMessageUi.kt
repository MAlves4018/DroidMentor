package ui.chat.model

/** Temporary UI message model; FLOW-002 later introduces the stable domain message identity. */
data class ChatMessageUi(
    val id: String,
    val role: ChatMessageRole,
    val content: String,
    val attachments: List<ChatAttachmentUi> = emptyList(),
    val canEdit: Boolean = role == ChatMessageRole.USER && content.isNotBlank(),
)
