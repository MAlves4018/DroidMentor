package data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import data.local.room.ConversationEntity
import data.local.room.DroidMentorDatabase
import data.local.room.MessageEntity
import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomConversationRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: DroidMentorDatabase
    private lateinit var repository: RoomConversationRepository
    private lateinit var databaseName: String

    @Before
    fun setup() {
        context = InstrumentationRegistry
            .getInstrumentation().targetContext

        databaseName = "room-test-${UUID.randomUUID()}.db"

        openDatabase()
    }

    @After
    fun cleanup() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            DroidMentorDatabase::class.java,
            databaseName
        ).build()

        repository = RoomConversationRepository(database)

        runBlocking {
            repository.initialize()
        }
    }

    private fun message(
        id: String,
        sequence: Long,
        content: String,
        role: Role = Role.USER
    ) = Message(
        id = MessageId(id),
        role = role,
        content = content,
        sequence = sequence
    )

    // Existing tests

    @Test
    fun multipleConversationsArePersisted() = runBlocking {
        val first = repository.create()
        val second = repository.create()

        assertNotEquals(first.id, second.id)
        assertEquals(2, repository.conversations.size)

        assertTrue(repository.activate(first.id))
        assertEquals(first.id, repository.activeId)

        assertNotNull(repository.find(first.id))
        assertNotNull(repository.find(second.id))
    }

    @Test
    fun messagesAreStoredInDeterministicOrder() = runBlocking {
        val conversation = repository.create()

        val updated = conversation
            .append(message("m1", 0, "Hello"))
            .append(message("m2", 1, "Hi!", Role.MODEL))
            .append(message("m3", 2, "Explain Room"))

        repository.save(updated)

        val loaded = repository.find(conversation.id)!!

        assertEquals(updated, loaded)
        assertEquals(
            listOf(0L, 1L, 2L),
            loaded.messages.map { it.sequence }
        )
    }

    @Test
    fun rewritingRemovesSubsequentMessages() = runBlocking {
        val conversation = repository.create()
            .append(message("m1", 0, "Hello"))
            .append(message("m2", 1, "Hi!", Role.MODEL))
            .append(message("m3", 2, "Explain Kotlin"))

        repository.save(conversation)

        val rewritten = conversation.rewriteUserMessage(
            MessageId("m1"),
            "Hello again"
        )

        repository.save(rewritten)

        val loaded = repository.find(conversation.id)!!

        assertEquals(1, loaded.messages.size)
        assertEquals("Hello again", loaded.messages[0].content)
        assertEquals(MessageId("m1"), loaded.messages[0].id)
    }

    @Test
    fun conversationsSurviveDatabaseReopening() = runBlocking {
        val conversation = repository.create()
        val updated = conversation.append(
            message("m1", 0, "Persisted message")
        )

        repository.save(updated)

        database.close()
        openDatabase()

        assertEquals(updated, repository.find(conversation.id))
        assertEquals(conversation.id, repository.activeId)
        assertEquals(conversation.id, repository.resumableId())
    }

    @Test
    fun deletingConversationDeletesMessages() = runBlocking {
        val conversation = repository.create()

        repository.save(
            conversation.append(message("m1", 0, "Hello"))
        )

        repository.delete(conversation.id)

        assertNull(repository.find(conversation.id))
        assertNull(repository.activeId)

        val messages = database.messageDao()
            .findByConversationId(conversation.id.value)

        assertTrue(messages.isEmpty())
    }

    @Test
    fun failedTransactionRollsBackAllWrites() {
        val conversation = ConversationEntity(
            id = "atomic-test",
            createdAtMillis = 1L,
            updatedAtMillis = 1L
        )

        val validMessage = MessageEntity(
            id = "valid",
            conversationId = "atomic-test",
            role = "USER",
            content = "Hello",
            sequence = 0L,
            createdAtMillis = 1L
        )

        val invalidMessage = MessageEntity(
            id = "invalid",
            conversationId = "missing-parent",
            role = "USER",
            content = "Invalid",
            sequence = 1L,
            createdAtMillis = 1L
        )

        val failed = runCatching {
            runBlocking(Dispatchers.IO) {
                database.withTransaction {
                    database.conversationDao().upsert(conversation)
                    database.messageDao().upsertAll(
                        listOf(validMessage, invalidMessage)
                    )
                }
            }
        }.isFailure

        assertTrue(failed)

        val persisted = runBlocking(Dispatchers.IO) {
            database.conversationDao().findById("atomic-test")
        }

        assertNull(persisted)
    }

    @Test
    fun deletingInactiveConversationPreservesActiveChatAndMessagesAfterReopen() =
        runBlocking {

            val inactive = repository.create()
            repository.save(
                inactive.append(
                    message("db002-a1", 0L, "First chat")
                )
            )

            val active = repository.create()
            val savedActive = active
                .append(message("db002-b1", 0L, "Second chat"))
                .append(message("db002-b2", 1L, "Second reply", Role.MODEL))

            repository.save(savedActive)
            repository.delete(inactive.id)

            database.close()
            openDatabase()

            assertNull(repository.find(inactive.id))
            assertEquals(savedActive, repository.find(active.id))
            assertEquals(active.id, repository.activeId)
            assertEquals(active.id, repository.resumableId())

            val removedMessages = database.messageDao()
                .findByConversationId(inactive.id.value)

            assertTrue(removedMessages.isEmpty())
        }

    // New persisted rewrite tests

    @Test
    fun persistedRewritePreservesPrefixAndRemovesSuffix() = runBlocking {
        val conversation = repository.create()
            .append(message("u1", 0, "First question"))
            .append(message("a1", 1, "First answer", Role.MODEL))
            .append(message("u2", 2, "Second question"))
            .append(message("a2", 3, "Second answer", Role.MODEL))
            .append(message("u3", 4, "Third question"))

        repository.save(conversation)

        // Modify the persisted prefix without updating the repository cache.
        // This proves the rewrite reads its prefix from Room.
        database.messageDao().updateUserContent(
            conversation.id.value,
            "u1",
            "First question updated in Room"
        )

        val rewritten = repository.rewriteUserMessage(
            conversation.id,
            MessageId("u2"),
            "Rewritten second question"
        )

        assertEquals(
            listOf("u1", "a1", "u2"),
            rewritten.messages.map { it.id.value }
        )

        assertEquals(
            listOf(
                "First question updated in Room",
                "First answer",
                "Rewritten second question"
            ),
            rewritten.messages.map { it.content }
        )

        assertEquals(
            listOf(0L, 1L, 2L),
            rewritten.messages.map { it.sequence }
        )

        assertEquals(rewritten, repository.find(conversation.id))
    }

    @Test
    fun persistedRewriteSurvivesDatabaseReopening() = runBlocking {
        val conversation = repository.create()
            .append(message("u1", 0, "Original"))
            .append(message("a1", 1, "Answer", Role.MODEL))
            .append(message("u2", 2, "Follow-up"))

        repository.save(conversation)

        val rewritten = repository.rewriteUserMessage(
            conversation.id,
            MessageId("u1"),
            "Changed question"
        )

        database.close()
        openDatabase()

        val loaded = repository.find(conversation.id)!!

        assertEquals(rewritten, loaded)
        assertEquals(1, loaded.messages.size)
        assertEquals("Changed question", loaded.messages.single().content)
        assertEquals(conversation.id, repository.activeId)

        val stored = database.messageDao()
            .findByConversationId(conversation.id.value)

        assertEquals(1, stored.size)
    }

    @Test
    fun persistedRewritePreservesMessageIdentityAndTimestamp() = runBlocking {
        val conversation = repository.create()
            .append(message("u1", 0, "Original question"))
            .append(message("a1", 1, "Original answer", Role.MODEL))

        repository.save(conversation)

        val before = database.messageDao()
            .findById(conversation.id.value, "u1")!!

        repository.rewriteUserMessage(
            conversation.id,
            MessageId("u1"),
            "Updated question"
        )

        val after = database.messageDao()
            .findById(conversation.id.value, "u1")!!

        assertEquals(before.id, after.id)
        assertEquals(before.sequence, after.sequence)
        assertEquals(before.createdAtMillis, after.createdAtMillis)
        assertEquals(before.role, after.role)
        assertEquals("Updated question", after.content)
    }

    @Test
    fun persistedRewriteRejectsModelMessageWithoutChanges() = runBlocking {
        val conversation = repository.create()
            .append(message("u1", 0, "Question"))
            .append(message("a1", 1, "Answer", Role.MODEL))

        repository.save(conversation)

        val failed = runCatching {
            repository.rewriteUserMessage(
                conversation.id,
                MessageId("a1"),
                "Invalid edit"
            )
        }.isFailure

        assertTrue(failed)
        assertEquals(conversation, repository.find(conversation.id))

        val stored = database.messageDao()
            .findByConversationId(conversation.id.value)

        assertEquals(2, stored.size)
        assertEquals("Answer", stored.last().content)
    }

    @Test
    fun persistedRewriteRejectsInvalidTargetsWithoutChanges() = runBlocking {
        val first = repository.create()
            .append(message("first-user", 0, "First conversation"))

        repository.save(first)

        val second = repository.create()
            .append(message("second-user", 0, "Second conversation"))

        repository.save(second)

        val wrongConversation = runCatching {
            repository.rewriteUserMessage(
                first.id,
                MessageId("second-user"),
                "Should fail"
            )
        }.isFailure

        val missingMessage = runCatching {
            repository.rewriteUserMessage(
                first.id,
                MessageId("missing"),
                "Should fail"
            )
        }.isFailure

        val blankContent = runCatching {
            repository.rewriteUserMessage(
                first.id,
                MessageId("first-user"),
                "   "
            )
        }.isFailure

        assertTrue(wrongConversation)
        assertTrue(missingMessage)
        assertTrue(blankContent)

        assertEquals(first, repository.find(first.id))
        assertEquals(second, repository.find(second.id))
    }

    @Test
    fun failedRewriteTransactionRestoresOriginalMessages() = runBlocking {
        val conversation = repository.create()
            .append(message("u1", 0, "Original question"))
            .append(message("a1", 1, "Original answer", Role.MODEL))
            .append(message("u2", 2, "Another question"))

        repository.save(conversation)

        // Simulate a database failure after the update and suffix deletion.
        val failed = runCatching {
            database.withTransaction {
                database.messageDao().updateUserContent(
                    conversation.id.value,
                    "u1",
                    "Temporary rewrite"
                )

                database.messageDao().deleteAfterSequence(
                    conversation.id.value,
                    0L
                )

                error("Simulated transaction failure")
            }
        }.isFailure

        assertTrue(failed)

        database.close()
        openDatabase()

        // Both the update and deletion must have rolled back.
        assertEquals(
            conversation,
            repository.find(conversation.id)
        )
    }
}
