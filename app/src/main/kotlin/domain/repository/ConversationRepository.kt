package domain.repository

import domain.model.Conversation
import domain.model.identity.ConversationId

interface ConversationRepository {
    val conversations: List<Conversation>
    val activeId: ConversationId?
    fun find(id: ConversationId): Conversation?
    fun create(): Conversation
    fun activate(id: ConversationId): Boolean
    fun save(conversation: Conversation)
    fun delete(id: ConversationId)
    fun leaveActive()
    fun resumableId(): ConversationId?
}