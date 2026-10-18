package test.fakes

import domain.model.identity.ConversationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import test.fixtures.ConversationFixtures

class FakeConversationRepositoryTest {
    @Test fun create_isDeterministicAndNewestFirst() {
        val fake = FakeConversationRepository()
        val a = fake.create()
        val b = fake.create()
        assertEquals("fake-conversation-1", a.id.value)
        assertEquals("fake-conversation-2", b.id.value)
        assertEquals(listOf(b, a), fake.conversations)
        assertEquals(b.id, fake.resumableId())
    }

    @Test fun initialFixtures_areVisibleAndActivatable() {
        val a = ConversationFixtures.conversation("A", "First", "Second")
        val b = ConversationFixtures.conversation("B", "Other")
        val fake = FakeConversationRepository(listOf(a, b), a.id)
        assertEquals(listOf(a, b), fake.conversations)
        assertEquals(2, fake.find(a.id)?.messages?.size)
        assertTrue(fake.activate(b.id))
        assertEquals(b.id, fake.activeId)
    }

    @Test fun updateAndDelete_produceInspectableCalls() {
        val a = ConversationFixtures.conversation("A")
        val fake = FakeConversationRepository(listOf(a), a.id)
        val updated = ConversationFixtures.conversation("A", "Edited")
        fake.save(updated)
        fake.delete(a.id)
        assertEquals(listOf(a.id), fake.savedIds)
        assertEquals(listOf(a.id), fake.deletedIds)
        assertNull(fake.find(a.id))
        assertNull(fake.resumableId())
    }

    @Test fun nonexistentId_doesNotBecomeActive() {
        val fake = FakeConversationRepository()
        assertFalse(fake.activate(ConversationId("missing")))
        assertNull(fake.activeId)
    }

    @Test fun saveUnknown_isRejected() {
        val fake = FakeConversationRepository()
        var rejected = false
        try {
            fake.save(ConversationFixtures.conversation("not-created"))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
