package domain.model

import domain.model.identity.ConversationId
import domain.model.identity.MessageId

data class Conversation(
    val id: ConversationId,
    val messages: List<Message> = emptyList(),
) {
    init {
        require(messages.map { it.id }.distinct().size == messages.size) {
            "Conversation message IDs must be unique."
        }
        require(
            messages.zipWithNext().all { (first, second) ->
                first.sequence < second.sequence
            }
        ) {
            "Conversation messages must have a strictly increasing deterministic sequence."
        }
    }

    val nextSequence: Long
        get() = (messages.lastOrNull()?.sequence ?: -1L) + 1L

    fun append(message: Message): Conversation {
        require(messages.none { it.id == message.id }) {
            "Message ID already exists in conversation."
        }
        require(
            messages.lastOrNull()?.sequence?.let { message.sequence > it } ?: true
        ) {
            "Appended message must come after the existing conversation order."
        }

        return copy(messages = messages + message)
    }

    fun rewriteUserMessage(
        messageId: MessageId,
        replacementText: String,
    ): Conversation {
        val replacement = replacementText.trim()

        require(replacement.isNotEmpty()) {
            "Replacement text cannot be blank."
        }

        val index = messages.indexOfFirst { it.id == messageId }
        require(index >= 0) {
            "Message does not belong to this conversation."
        }

        val target = messages[index]
        require(target.role == Role.USER) {
            "Only user messages can be rewritten."
        }

        val rewritten = target.copy(content = replacement)
        return copy(messages = messages.take(index) + rewritten)
    }
}
