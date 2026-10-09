package domain.model

import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConversationTest {

    @Test
    fun append_preservesStableIdsExplicitRolesAndDeterministicOrder() {
        val conversation = Conversation(ConversationId("c1"))
            .append(message("u1", Role.USER, "Question", 0))
            .append(message("m1", Role.MODEL, "Answer", 1))

        assertEquals(
            listOf("u1", "m1"),
            conversation.messages.map { it.id.value },
        )
        assertEquals(
            listOf(Role.USER, Role.MODEL),
            conversation.messages.map { it.role },
        )
        assertEquals(
            listOf(0L, 1L),
            conversation.messages.map { it.sequence },
        )
    }

    @Test
    fun rewriteFirstUserMessage_keepsIdentityAndDropsTheSuffix() {
        val rewritten = sampleConversation().rewriteUserMessage(
            messageId = MessageId("u1"),
            replacementText = "Rewritten first question",
        )

        assertEquals(1, rewritten.messages.size)
        assertEquals("u1", rewritten.messages.single().id.value)
        assertEquals(0L, rewritten.messages.single().sequence)
        assertEquals("Rewritten first question", rewritten.messages.single().content)
    }

    @Test
    fun rewriteMiddleUserMessage_keepsPrefixAndDropsOnlyFollowingMessages() {
        val rewritten = sampleConversation().rewriteUserMessage(
            messageId = MessageId("u2"),
            replacementText = "Rewritten second question",
        )

        assertEquals(
            listOf("u1", "m1", "u2"),
            rewritten.messages.map { it.id.value },
        )
        assertEquals("Rewritten second question", rewritten.messages.last().content)
    }

    @Test
    fun rewriteLatestUserMessage_keepsEarlierConversationIntact() {
        val conversation = sampleConversation().append(
            message("u3", Role.USER, "Latest question", 4)
        )

        val rewritten = conversation.rewriteUserMessage(
            messageId = MessageId("u3"),
            replacementText = "Edited latest question",
        )

        assertEquals(
            listOf("u1", "m1", "u2", "m2", "u3"),
            rewritten.messages.map { it.id.value },
        )
        assertEquals("Edited latest question", rewritten.messages.last().content)
    }

    @Test
    fun rewritingOneConversation_doesNotMutateAnotherConversation() {
        val first = sampleConversation()
        val second = Conversation(ConversationId("c2")).append(
            message("other-u1", Role.USER, "Independent", 0)
        )

        val rewrittenFirst = first.rewriteUserMessage(
            messageId = MessageId("u2"),
            replacementText = "Changed",
        )

        assertNotEquals(first, rewrittenFirst)
        assertEquals("Independent", second.messages.single().content)
        assertEquals("other-u1", second.messages.single().id.value)
    }

    private fun sampleConversation(): Conversation =
        Conversation(ConversationId("c1"))
            .append(message("u1", Role.USER, "First question", 0))
            .append(message("m1", Role.MODEL, "First answer", 1))
            .append(message("u2", Role.USER, "Second question", 2))
            .append(message("m2", Role.MODEL, "Second answer", 3))

    private fun message(
        id: String,
        role: Role,
        content: String,
        sequence: Long,
    ) = Message(
        id = MessageId(id),
        role = role,
        content = content,
        sequence = sequence,
    )
}
