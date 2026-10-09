package domain.model

import domain.model.identity.MessageId

data class Message(
    val id: MessageId,
    val role: Role,
    val content: String,
    val sequence: Long,
) {
    init {
        require(content.isNotBlank()) {
            "Message content cannot be blank."
        }
        require(sequence >= 0L) {
            "Message sequence cannot be negative."
        }
    }
}
