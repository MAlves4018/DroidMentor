package domain.model

import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

/** Deterministic bounded property campaign. The seed is printed on assertion failure. */
class ConversationRewritePropertyTest {

    @Test
    fun rewritePreservesIdentityPrefixAndStrictOrderAcross128SeededHistories() {
        repeat(128) { index ->
            val seed = 803_000 + index
            val random = Random(seed)
            val pairs = random.nextInt(1, 30)
            val original = Conversation(ConversationId("case-$seed"))
            var conversation = original
            var nextSequence = 0L
            repeat(pairs) { turn ->
                conversation = conversation.append(
                    Message(MessageId("u-$turn"), Role.USER, "Question $turn", nextSequence)
                )
                nextSequence += random.nextLong(1L, 4L)
                conversation = conversation.append(
                    Message(MessageId("a-$turn"), Role.MODEL, "Answer $turn", nextSequence)
                )
                nextSequence += random.nextLong(1L, 4L)
            }
            val before = conversation
            val selectedTurn = random.nextInt(pairs)
            val selectedIndex = selectedTurn * 2
            val targetId = before.messages[selectedIndex].id
            val expected = before.messages.take(selectedIndex) +
                before.messages[selectedIndex].copy(content = "Updated $seed")
            val actual = conversation.rewriteUserMessage(targetId, "  Updated $seed  ")

            assertEquals("seed=$seed identity", before.id, actual.id)
            assertEquals("seed=$seed prefix", expected, actual.messages)
            assertEquals("seed=$seed ordering", true,
                actual.messages.zipWithNext().all { (a, b) -> a.sequence < b.sequence }
            )
            assertEquals("seed=$seed original immutable", before, conversation)
            assertFalse("seed=$seed old suffix retained", actual.messages.size > selectedIndex + 1)
        }
    }

    @Test
    fun invalidTargetsDoNotMutateOriginalConversation() {
        val original = Conversation(ConversationId("invalid-rewrite"))
            .append(Message(MessageId("u1"), Role.USER, "Question", 0L))
            .append(Message(MessageId("a1"), Role.MODEL, "Answer", 1L))
        val before = original.copy()

        assertThrows(IllegalArgumentException::class.java) {
            original.rewriteUserMessage(MessageId("unknown"), "Change")
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.rewriteUserMessage(MessageId("a1"), "Change")
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.rewriteUserMessage(MessageId("u1"), "  ")
        }
        assertEquals(before, original)
    }
}
