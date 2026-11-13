package data.local.room

import domain.model.Conversation
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId

// Room -> Domain

fun MessageEntity.toDomain(): Message =
    Message(
        id = MessageId(id),
        role = Role.valueOf(role),
        content = content,
        sequence = sequence
    )

fun ConversationWithMessages.toDomain(): Conversation =
    Conversation(
        id = ConversationId(conversation.id),
        messages = messages
            .sortedBy { it.sequence }
            .map { it.toDomain() }
    )

// Domain -> Room

fun Conversation.toEntity(
    createdAtMillis: Long,
    updatedAtMillis: Long,
    isActive: Boolean
): ConversationEntity =
    ConversationEntity(
        id = id.value,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        isActive = isActive
    )

fun Message.toEntity(
    conversationId: String,
    createdAtMillis: Long
): MessageEntity =
    MessageEntity(
        id = id.value,
        conversationId = conversationId,
        role = role.name,
        content = content,
        sequence = sequence,
        createdAtMillis = createdAtMillis
    )