package domain.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import domain.model.Conversation
import domain.model.identity.ConversationId
import java.util.UUID

/** Later replace this implementation with Room without changing the navigation/UI contracts. */
class InMemoryConversationRepository : ConversationRepository {
    override suspend fun initialize() = Unit
    private val items = mutableStateListOf<Conversation>()
    private val selection = mutableStateOf<ConversationId?>(null)
    override val conversations: List<Conversation> get() = items
    override val activeId: ConversationId? get() = selection.value
    override fun find(id: ConversationId) = items.firstOrNull { it.id == id }
    override suspend fun create(): Conversation = Conversation(ConversationId(UUID.randomUUID().toString())).also {
        items.add(0, it)
        selection.value = it.id
    }
    override suspend fun activate(id: ConversationId): Boolean {
        if (find(id) == null) return false
        selection.value = id
        return true
    }
    override suspend fun save(conversation: Conversation) {
        val index = items.indexOfFirst { it.id == conversation.id }
        require(index >= 0) { "Unknown conversation" }
        items.removeAt(index)
        items.add(0, conversation)
    }
    override suspend fun delete(id: ConversationId) {
        items.removeAll { it.id == id }
        if (selection.value == id) selection.value = null
    }
    override suspend fun leaveActive() { selection.value = null }
    override fun resumableId(): ConversationId? = activeId?.takeIf { find(it) != null }
}
