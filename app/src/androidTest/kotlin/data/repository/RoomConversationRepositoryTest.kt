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

        val messages = runBlocking(Dispatchers.IO) {
            database.messageDao()
                .findByConversationId(conversation.id.value)
        }

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
}