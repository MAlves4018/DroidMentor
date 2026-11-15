package domain.repository

import domain.model.Conversation
import domain.model.identity.ConversationId

interface ConversationRepository {

    val conversations: List<Conversation>
    val activeId: ConversationId?

    suspend fun initialize()

    fun find(id: ConversationId): Conversation?

    suspend fun create(): Conversation

    suspend fun activate(id: ConversationId): Boolean

    suspend fun save(conversation: Conversation)

    suspend fun delete(id: ConversationId)

    suspend fun leaveActive()

    fun resumableId(): ConversationId?
}