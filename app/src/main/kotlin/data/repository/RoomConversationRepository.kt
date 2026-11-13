package data.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.room.withTransaction
import data.local.room.DroidMentorDatabase
import data.local.room.toDomain
import data.local.room.toEntity
import domain.model.Conversation
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class RoomConversationRepository(
    private val database: DroidMentorDatabase
) : ConversationRepository {

    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    private val mutex = Mutex()

    private val items = mutableStateListOf<Conversation>()
    private val selection = mutableStateOf<ConversationId?>(null)

    override val conversations: List<Conversation>
        get() = items

    override val activeId: ConversationId?
        get() = selection.value

    override fun find(id: ConversationId): Conversation? =
        items.firstOrNull { it.id == id }

    override suspend fun initialize() {
        mutex.withLock {
            refresh()
        }
    }

    override suspend fun create(): Conversation = mutex.withLock {
        val conversation = Conversation(
            id = ConversationId(UUID.randomUUID().toString())
        )

        withContext(Dispatchers.IO) {
            database.withTransaction {
                val now = System.currentTimeMillis()

                conversationDao.clearActive()

                conversationDao.upsert(
                    conversation.toEntity(
                        createdAtMillis = now,
                        updatedAtMillis = now,
                        isActive = true
                    )
                )
            }
        }

        refresh()
        conversation
    }

    override suspend fun activate(id: ConversationId): Boolean =
        mutex.withLock {
            val activated = withContext(Dispatchers.IO) {
                database.withTransaction {
                    if (conversationDao.findById(id.value) == null) {
                        false
                    } else {
                        conversationDao.clearActive()
                        conversationDao.setActive(id.value) == 1
                    }
                }
            }

            if (activated) refresh()
            activated
        }

    override suspend fun save(conversation: Conversation) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                database.withTransaction {
                    val existing = conversationDao.findById(
                        conversation.id.value
                    ) ?: error("Unknown conversation")

                    val now = System.currentTimeMillis()

                    val existingMessages = messageDao
                        .findByConversationId(conversation.id.value)
                        .associateBy { it.id }

                    val messageIds = conversation.messages.map {
                        it.id.value
                    }

                    if (messageIds.isEmpty()) {
                        messageDao.deleteAfterSequence(
                            conversation.id.value,
                            -1L
                        )
                    } else {
                        messageDao.deleteExcept(
                            conversation.id.value,
                            messageIds
                        )
                    }

                    messageDao.upsertAll(
                        conversation.messages.map { message ->
                            message.toEntity(
                                conversationId = conversation.id.value,
                                createdAtMillis =
                                    existingMessages[message.id.value]
                                        ?.createdAtMillis ?: now
                            )
                        }
                    )

                    conversationDao.upsert(
                        conversation.toEntity(
                            createdAtMillis = existing.createdAtMillis,
                            updatedAtMillis = now,
                            isActive = existing.isActive
                        )
                    )
                }
            }

            refresh()
        }
    }

    override suspend fun delete(id: ConversationId) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                database.withTransaction {
                    conversationDao.deleteById(id.value)
                }
            }

            refresh()
        }
    }

    override suspend fun leaveActive() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                database.withTransaction {
                    conversationDao.clearActive()
                }
            }

            refresh()
        }
    }

    override fun resumableId(): ConversationId? =
        activeId?.takeIf { find(it) != null }

    private suspend fun refresh() {
        val (loaded, active) = withContext(Dispatchers.IO) {
            database.withTransaction {
                val conversations = conversationDao
                    .observeAll()
                    .first()
                    .map { entity ->
                        Conversation(
                            id = ConversationId(entity.id),
                            messages = messageDao
                                .findByConversationId(entity.id)
                                .map { it.toDomain() }
                        )
                    }

                val activeId = conversationDao
                    .findActive()
                    ?.id
                    ?.let(::ConversationId)

                conversations to activeId
            }
        }

        withContext(Dispatchers.Main.immediate) {
            items.clear()
            items.addAll(loaded)
            selection.value = active
        }
    }
}