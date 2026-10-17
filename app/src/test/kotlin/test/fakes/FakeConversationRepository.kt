package test.fakes

import domain.model.Conversation
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository

/** Deterministic, dependency-free fake shared by ARCH-002 presentation tests. */
class FakeConversationRepository(
    initialConversations: List<Conversation> = emptyList(),
    initialActiveId: ConversationId? = null,
) : ConversationRepository {
    private val entries = initialConversations.toMutableList()
    private var selection = initialActiveId
    private var nextId = 0

    init {
        require(entries.map { it.id }.distinct().size == entries.size)
        require(initialActiveId == null || entries.any { it.id == initialActiveId })
    }

    val savedIds = mutableListOf<ConversationId>()
    val deletedIds = mutableListOf<ConversationId>()

    override val conversations: List<Conversation> get() = entries.toList()
    override val activeId: ConversationId? get() = selection

    override fun find(id: ConversationId): Conversation? = entries.firstOrNull { it.id == id }

    override fun create(): Conversation {
        val id = generateSequence { ConversationId("fake-conversation-${++nextId}") }
            .first { candidate -> find(candidate) == null }
        val conversation = Conversation(id)
        entries.add(0, conversation)
        selection = id
        return conversation
    }

    override fun activate(id: ConversationId): Boolean {
        if (find(id) == null) return false
        selection = id
        return true
    }

    override fun save(conversation: Conversation) {
        val index = entries.indexOfFirst { it.id == conversation.id }
        require(index >= 0) { "Unknown conversation" }
        entries.removeAt(index)
        entries.add(0, conversation)
        savedIds.add(conversation.id)
    }

    override fun delete(id: ConversationId) {
        entries.removeAll { it.id == id }
        if (selection == id) selection = null
        deletedIds.add(id)
    }

    override fun leaveActive() { selection = null }

    override fun resumableId(): ConversationId? = selection?.takeIf { find(it) != null }
}
