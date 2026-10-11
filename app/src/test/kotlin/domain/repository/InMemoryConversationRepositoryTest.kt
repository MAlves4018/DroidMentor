package domain.repository

import domain.model.Conversation
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import org.junit.Assert.*
import org.junit.Test

class InMemoryConversationRepositoryTest {
    private fun message(text: String, sequence: Long = 0) = Message(
        MessageId("message-$sequence"), Role.USER, text, sequence
    )

    @Test fun emptyRepositoryHasNoActiveOrResumableConversation() {
        val repo = InMemoryConversationRepository()
        assertTrue(repo.conversations.isEmpty())
        assertNull(repo.activeId)
        assertNull(repo.resumableId())
    }

    @Test fun createAddsUniqueConversationsNewestFirstAndActivatesNewest() {
        val repo = InMemoryConversationRepository()
        val first = repo.create()
        val second = repo.create()
        assertNotEquals(first.id, second.id)
        assertEquals(listOf(second, first), repo.conversations)
        assertEquals(second.id, repo.activeId)
        assertEquals(second.id, repo.resumableId())
    }

    @Test fun selectingHistoryConversationMakesItActiveWithoutDuplicatingIt() {
        val repo = InMemoryConversationRepository()
        val first = repo.create()
        repo.create()
        assertTrue(repo.activate(first.id))
        assertEquals(first.id, repo.activeId)
        assertEquals(2, repo.conversations.size)
        assertEquals(first, repo.find(first.id))
    }

    @Test fun unknownConversationCannotBeActivated() {
        val repo = InMemoryConversationRepository()
        val existing = repo.create()
        assertFalse(repo.activate(ConversationId("missing")))
        assertEquals(existing.id, repo.activeId)
        assertNull(repo.find(ConversationId("missing")))
    }

    @Test fun saveUpdatesConversationWithoutChangingItsIdentity() {
        val repo = InMemoryConversationRepository()
        val first = repo.create()
        val second = repo.create()
        val updated = first.append(message("Hello"))
        repo.save(updated)
        assertEquals(updated, repo.find(first.id))
        assertEquals(listOf(updated, second), repo.conversations)
        assertEquals(second.id, repo.activeId)
    }

    @Test fun savingUnknownConversationFailsWithoutCreatingIt() {
        val repo = InMemoryConversationRepository()
        try {
            repo.save(Conversation(ConversationId("unknown")))
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
        assertTrue(repo.conversations.isEmpty())
    }

    @Test fun deleteInactivePreservesActiveAndOtherConversations() {
        val repo = InMemoryConversationRepository()
        val first = repo.create()
        val second = repo.create()
        repo.delete(first.id)
        assertEquals(listOf(second), repo.conversations)
        assertEquals(second.id, repo.resumableId())
    }

    @Test fun deleteActiveClearsSelectionWithoutSelectingAnother() {
        val repo = InMemoryConversationRepository()
        val first = repo.create()
        val second = repo.create()
        repo.delete(second.id)
        assertEquals(listOf(first), repo.conversations)
        assertNull(repo.activeId)
        assertNull(repo.resumableId())
        assertFalse(repo.activate(second.id))
    }

    @Test fun deletingUnknownConversationIsNoOp() {
        val repo = InMemoryConversationRepository()
        val first = repo.create()
        repo.delete(ConversationId("missing"))
        assertEquals(listOf(first), repo.conversations)
        assertEquals(first.id, repo.activeId)
    }

    @Test fun leaveActiveKeepsHistoryButDisablesResume() {
        val repo = InMemoryConversationRepository()
        val first = repo.create()
        repo.leaveActive()
        assertNull(repo.activeId)
        assertNull(repo.resumableId())
        assertEquals(first, repo.find(first.id))
        assertTrue(repo.activate(first.id))
        assertEquals(first.id, repo.resumableId())
    }

    @Test fun newRepositoryInstanceDoesNotPersistData() {
        val original = InMemoryConversationRepository()
        original.create()
        val restarted = InMemoryConversationRepository()
        assertTrue(restarted.conversations.isEmpty())
        assertNull(restarted.resumableId())
    }
}
