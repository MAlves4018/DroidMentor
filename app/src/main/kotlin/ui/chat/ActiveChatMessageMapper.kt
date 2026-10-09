package ui.chat

import domain.model.Conversation
import domain.model.Message
import domain.model.Role
import ui.chat.model.ChatMessageRole
import ui.chat.model.ChatMessageUi

internal fun Conversation.toChatMessagesUi(): List<ChatMessageUi> =
    messages.map { it.toChatMessageUi() }

private fun Message.toChatMessageUi(): ChatMessageUi =
    ChatMessageUi(
        id = id.value,
        role = when (role) {
            Role.USER -> ChatMessageRole.USER
            Role.MODEL -> ChatMessageRole.MODEL
        },
        content = content,
        canEdit = role == Role.USER,
    )
