package test.fixtures

import domain.model.Conversation
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId

/** Stable IDs and ordering for repository, ViewModel, and navigation tests. */
object ConversationFixtures {
    fun conversation(id: String = "conversation-A", vararg content: String): Conversation =
        Conversation(
            id = ConversationId(id),
            messages = content.mapIndexed { index, text ->
                Message(
                    id = MessageId("$id-message-$index"),
                    role = Role.USER,
                    content = text,
                    sequence = index.toLong(),
                )
            },
        )
}
