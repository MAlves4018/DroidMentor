package data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import data.local.room.DroidMentorDatabase
import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import domain.model.identity.ConversationId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import platform.navigation.DroidMentorDestination
import platform.navigation.DroidMentorNavigator
import ui.historyChat.ChatHistoryAction
import ui.historyChat.HistoryEffect
import ui.historyChat.HistoryViewModel
import ui.navigation.NavigationAction
import java.util.UUID

/** DB-002: real Room persistence + History + navigation, without a Gemini provider. */
@RunWith(AndroidJUnit4::class)
class RoomHistoryLifecycleIntegrationTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var database: DroidMentorDatabase
    private lateinit var repository: RoomConversationRepository

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseName = "db002-${UUID.randomUUID()}.db"
        database = newDatabase()
        repository = RoomConversationRepository(database)
        runBlocking { repository.initialize() }
    }

    @After
    fun cleanup() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun newDatabase(): DroidMentorDatabase = Room.databaseBuilder(
        context, DroidMentorDatabase::class.java, databaseName
    ).build()

    private suspend fun reopen() {
        database.close()
        database = newDatabase()
        repository = RoomConversationRepository(database)
        repository.initialize()
    }

    private fun message(id: String, sequence: Long, text: String, role: Role = Role.USER) =
        Message(id = MessageId(id), role = role, content = text, sequence = sequence)

    @Test
    fun restartRestoresHistoryAndResumableChatWithoutNetwork() = runBlocking {
        val first = repository.create()
        repository.save(first.append(message("first-0", 0, "A saved question")))
        val second = repository.create()
        repository.save(
            second.append(message("second-0", 0, "Another question"))
                .append(message("second-1", 1, "Saved answer", Role.MODEL))
        )

        reopen() // New repository, same SQLite file; no network or MentorProvider.

        assertEquals(second.id, repository.resumableId())
        assertEquals(2, repository.conversations.size)
        assertEquals(
            listOf("Another question", "Saved answer"),
            repository.find(second.id)!!.messages.map { it.content }
        )
        val history = HistoryViewModel(repository)
        val summaries = history.uiState.value.groups.flatMap { it.conversations }
        assertEquals(setOf(first.id.value, second.id.value), summaries.map { it.id }.toSet())
        assertEquals("Active", summaries.single { it.id == second.id.value }.lastActivity)

        val navigator = DroidMentorNavigator(
            initialBackStack = listOf(
                DroidMentorDestination.Title,
                DroidMentorDestination.ActiveChat(second.id),
            ),
            repository = repository,
        )
        navigator.synchronizeActiveConversation()
        assertEquals(second.id, navigator.currentConversationId)
        val effect = history.onAction(ChatHistoryAction.OpenConversation(first.id.value))
        assertEquals(
            HistoryEffect.Navigate(NavigationAction.OpenConversation(first.id)), effect
        )
        navigator.handle((effect as HistoryEffect.Navigate).action)
        assertEquals(first.id, navigator.currentConversationId)
        assertEquals("A saved question", repository.find(first.id)!!.messages.single().content)
    }

    @Test
    fun deletingActiveChatRemovesOnlyItsRowsAndFallsBackToTitle() = runBlocking {
        val retained = repository.create()
        repository.save(retained.append(message("keep-0", 0, "Keep this message")))
        val removed = repository.create()
        repository.save(removed.append(message("remove-0", 0, "Remove this message")))
        val navigator = DroidMentorNavigator(
            initialBackStack = listOf(
                DroidMentorDestination.Title,
                DroidMentorDestination.ActiveChat(removed.id),
            ),
            repository = repository,
        )

        val history = HistoryViewModel(repository)
        assertNull(history.onAction(ChatHistoryAction.RequestDeleteConversation(removed.id.value)))
        assertNotNull(history.uiState.value.pendingDeletion)
        assertNotNull(repository.find(removed.id)) // Confirmation is mandatory.
        assertEquals(
            HistoryEffect.ConversationDeleted(removed.id),
            history.onAction(ChatHistoryAction.ConfirmDeleteConversation),
        )
        navigator.removeConversation(removed.id)

        assertEquals(DroidMentorDestination.Title, navigator.currentDestination)
        assertNull(repository.activeId)
        assertNull(repository.resumableId())
        assertNull(repository.find(removed.id))
        assertEquals("Keep this message", repository.find(retained.id)!!.messages.single().content)
        withContext(Dispatchers.IO) {
            assertTrue(database.messageDao().findByConversationId(removed.id.value).isEmpty())
            assertEquals(1, database.messageDao().findByConversationId(retained.id.value).size)
        }

        reopen()
        assertNull(repository.find(removed.id))
        assertNull(repository.resumableId())
        assertEquals(1, repository.conversations.size)
        assertEquals("Keep this message", repository.find(retained.id)!!.messages.single().content)
    }

    @Test
    fun deletingInactiveChatKeepsActiveConversationAcrossRestart() = runBlocking {
        val inactive = repository.create()
        repository.save(inactive.append(message("old-0", 0, "Old question")))
        val active = repository.create()
        repository.save(active.append(message("new-0", 0, "Current question")))
        val history = HistoryViewModel(repository)
        history.onAction(ChatHistoryAction.RequestDeleteConversation(inactive.id.value))
        assertEquals(
            HistoryEffect.ConversationDeleted(inactive.id),
            history.onAction(ChatHistoryAction.ConfirmDeleteConversation),
        )
        reopen()
        assertEquals(active.id, repository.activeId)
        assertEquals(active.id, repository.resumableId())
        assertNull(repository.find(inactive.id))
        assertEquals("Current question", repository.find(active.id)!!.messages.single().content)
    }

    @Test
    fun leavingChatPreservesHistoryButStartupDoesNotResumeIt() = runBlocking {
        val saved = repository.create()
        repository.save(saved.append(message("saved-0", 0, "Still saved")))
        repository.leaveActive()
        reopen()
        assertNull(repository.resumableId())
        assertEquals("Still saved", repository.find(saved.id)!!.messages.single().content)
        val navigator = DroidMentorNavigator(repository = repository)
        assertEquals(DroidMentorDestination.Title, navigator.currentDestination)
        assertFalse(navigator.canGoBack)
        assertTrue(HistoryViewModel(repository).uiState.value.groups.any {
            group -> group.conversations.any { it.id == saved.id.value }
        })
    }

    @Test
    fun openingUnknownConversationDoesNotClearValidActiveSelection() = runBlocking {
        val saved = repository.create()
        repository.save(saved.append(message("valid-0", 0, "Retain active selection")))
        val unknownId = ConversationId("missing-${UUID.randomUUID()}")

        assertFalse(repository.activate(unknownId))
        assertEquals(saved.id, repository.activeId)
        reopen()
        assertEquals(saved.id, repository.resumableId())
        assertEquals("Retain active selection", repository.find(saved.id)!!.messages.single().content)
    }
}
