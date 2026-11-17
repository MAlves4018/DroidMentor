package data.repository

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import data.InMemorySettingsRepository
import data.local.room.DroidMentorDatabase
import domain.model.Message
import domain.model.Role
import domain.model.identity.ConversationId
import domain.model.identity.MessageId
import domain.provider.MentorProvider
import domain.provider.ProviderFailure
import domain.usecase.SendMessageUseCase
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ui.chat.ActiveChatAction
import ui.chat.ActiveChatViewModel
import ui.chat.model.ChatErrorKind
import ui.chat.model.ChatRequestState

/**
 * DB-003 integration evidence, running against a real Room SQLite file on Android.
 * The provider is deliberately controlled: this tests the entire local chain
 * without making nondeterministic network calls or requiring an actual API key.
 */
@RunWith(AndroidJUnit4::class)
class DB003RoomProviderChainTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var database: DroidMentorDatabase
    private lateinit var repository: RoomConversationRepository
    private var vm: ActiveChatViewModel? = null

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context, DroidMentorDatabase::class.java, databaseName
        ).build()
        repository = RoomConversationRepository(database)
        runBlocking { repository.initialize() }
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseName = "db003-chain-${UUID.randomUUID()}.db"
        openDatabase()
    }

    @After
    fun cleanup() {
        vm?.viewModelScope?.cancel()
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun reopen() {
        vm?.viewModelScope?.cancel()
        vm = null
        database.close()
        openDatabase()
    }

    private fun msg(id: String, role: Role, text: String, sequence: Long) =
        Message(MessageId(id), role, text, sequence)

    private class ControlledProvider : MentorProvider {
        data class Request(
            val history: List<Message>,
            val response: CompletableDeferred<String> = CompletableDeferred()
        )
        val requests = CopyOnWriteArrayList<Request>()

        override suspend fun generateReply(history: List<Message>, apiKey: String): String {
            check(apiKey == "local-test-key")
            val request = Request(history.toList())
            requests.add(request)
            return request.response.await()
        }
    }

    private fun startViewModel(id: ConversationId, provider: ControlledProvider): ActiveChatViewModel {
        val settings = InMemorySettingsRepository()
        runBlocking { settings.saveApiKey("local-test-key") }
        return ActiveChatViewModel(
            conversationId = id,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
        ).also { vm = it }
    }

    private fun action(action: ActiveChatAction) {
        val model = requireNotNull(vm)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model.onAction(action)
        }
    }

    private fun awaitCondition(label: String, predicate: () -> Boolean) {
        val limit = SystemClock.uptimeMillis() + 10_000L
        while (SystemClock.uptimeMillis() < limit) {
            if (predicate()) return
            SystemClock.sleep(25L)
        }
        throw AssertionError("Timed out while waiting for: $label")
    }

    private fun seed(): ConversationId = runBlocking {
        val id = repository.create().id
        repository.save(
            requireNotNull(repository.find(id))
                .append(msg("u1", Role.USER, "First question", 0L))
                .append(msg("a1", Role.MODEL, "First answer", 1L))
                .append(msg("u2", Role.USER, "Second question", 2L))
                .append(msg("a2", Role.MODEL, "OBSOLETE answer", 3L))
        )
        id
    }

    private fun confirmEditSecondMessage() {
        action(ActiveChatAction.EditMessageClicked("u2"))
        action(ActiveChatAction.EditDraftChanged("Updated second question"))
        action(ActiveChatAction.SaveEditClicked)
        action(ActiveChatAction.ConfirmRewriteClicked)
    }

    @Test
    fun editingViaViewModelUsesRoomHistoryAndPersistsRegeneratedAnswer() {
        val id = seed()
        val provider = ControlledProvider()
        val model = startViewModel(id, provider)
        confirmEditSecondMessage()

        awaitCondition("regeneration provider call") { provider.requests.size == 1 }
        val request = provider.requests.single()
        assertEquals(
            listOf("First question", "First answer", "Updated second question"),
            request.history.map { it.content }
        )
        assertEquals(request.history, requireNotNull(repository.find(id)).messages)
        assertFalse(request.history.any { it.content == "OBSOLETE answer" })

        request.response.complete("Regenerated answer")
        awaitCondition("Room persists model answer") {
            repository.find(id)?.messages?.lastOrNull()?.content == "Regenerated answer" &&
                model.uiState.value.requestState == ChatRequestState.Idle
        }
        reopen()

        assertEquals(
            listOf("First question", "First answer", "Updated second question", "Regenerated answer"),
            requireNotNull(repository.find(id)).messages.map { it.content }
        )
        assertEquals(id, repository.resumableId())
    }

    @Test
    fun failedRegenerationKeepsEditedPrefixAndRetryDoesNotRestoreOldSuffix() {
        val id = seed()
        val provider = ControlledProvider()
        val model = startViewModel(id, provider)
        confirmEditSecondMessage()

        awaitCondition("first regeneration attempt") { provider.requests.size == 1 }
        provider.requests[0].response.completeExceptionally(ProviderFailure.Offline)
        awaitCondition("offline UI error") {
            model.uiState.value.requestState ==
                ChatRequestState.Error(ChatErrorKind.OFFLINE)
        }
        assertEquals(
            listOf("First question", "First answer", "Updated second question"),
            requireNotNull(repository.find(id)).messages.map { it.content }
        )

        action(ActiveChatAction.RetryClicked)
        awaitCondition("retry request") { provider.requests.size == 2 }
        assertEquals(provider.requests[0].history, provider.requests[1].history)
        provider.requests[1].response.complete("Recovery reply")
        awaitCondition("recovery persisted") {
            repository.find(id)?.messages?.lastOrNull()?.content == "Recovery reply" &&
                model.uiState.value.requestState == ChatRequestState.Idle
        }
        reopen()
        assertEquals(
            listOf("First question", "First answer", "Updated second question", "Recovery reply"),
            requireNotNull(repository.find(id)).messages.map { it.content }
        )
    }

    @Test
    fun firstMiddleAndLatestRewriteHaveExactlyTheExpectedPersistedPrefix() = runBlocking {
        for (target in listOf(0, 2, 4)) {
            val created = repository.create()
            val entries = (0L..5L).map { index ->
                msg(
                    "$target-$index",
                    if (index % 2L == 0L) Role.USER else Role.MODEL,
                    "Original $target-$index",
                    index
                )
            }
            repository.save(created.copy(messages = entries))
            val result = repository.rewriteUserMessage(
                created.id,
                entries[target].id,
                "Updated $target"
            )
            val expected = entries.take(target) + entries[target].copy(content = "Updated $target")
            assertEquals(expected, result.messages)
            assertEquals(expected, database.messageDao()
                .findByConversationId(created.id.value)
                .map { domain.model.Message(
                    MessageId(it.id), Role.valueOf(it.role), it.content, it.sequence
                ) })
        }
        val beforeRestart = repository.conversations.associate { it.id to it.messages }
        reopen()
        for ((id, expected) in beforeRestart) {
            assertEquals(expected, repository.find(id)?.messages)
        }
    }

    @Test
    fun storageErrorInPublicRewriteRollsBackBothUpdateAndSuffixDeletion() = runBlocking {
        val id = seed()
        val original = requireNotNull(repository.find(id))
        // Trigger exists only in this throwaway test database. It forces the
        // public rewrite path to fail after updating the USER row, at delete.
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER db003_rewrite_fail BEFORE DELETE ON messages
            WHEN OLD.conversationId = '${id.value}'
            BEGIN SELECT RAISE(ABORT, 'injected rewrite failure'); END
        """.trimIndent())
        try {
            val error = runCatching {
                repository.rewriteUserMessage(id, MessageId("u1"), "Changed")
            }.exceptionOrNull()
            assertTrue("The injected trigger must abort the public rewrite", error != null)
        } finally {
            database.openHelper.writableDatabase.execSQL(
                "DROP TRIGGER IF EXISTS db003_rewrite_fail"
            )
        }
        assertEquals(original, repository.find(id))
        reopen()
        assertEquals(original, repository.find(id))
    }

    @Test
    fun moreThanSQLiteLegacyBindLimitRoundTripsAndCanRewritePrefix() = runBlocking {
        val created = repository.create()
        val entries = (0L..1024L).map { index ->
            msg("bulk-$index", Role.USER, "Message $index", index)
        }
        repository.save(created.copy(messages = entries))
        assertEquals(1025, requireNotNull(repository.find(created.id)).messages.size)

        // Exercise DB-001's >998-ID snapshot path, including a replacement.
        val updated = entries.toMutableList()
        updated[500] = updated[500].copy(content = "Corrected middle")
        repository.save(created.copy(messages = updated))
        assertEquals(updated, repository.find(created.id)?.messages)

        val rewritten = repository.rewriteUserMessage(
            created.id, MessageId("bulk-500"), "Shortened history"
        )
        assertEquals(501, rewritten.messages.size)
        assertEquals("Shortened history", rewritten.messages.last().content)
        reopen()
        assertEquals(rewritten, repository.find(created.id))
    }

}
