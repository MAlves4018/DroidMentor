
package domain.repository

import domain.model.Conversation
import domain.model.identity.ConversationId
import domain.model.identity.MessageId

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

    /**
     * Rewrites a USER message and removes all subsequent messages.
     *
     * Default implementation for in-memory and test repositories.
     * RoomConversationRepository will override this with an
     * atomic database transaction.
     */
    suspend fun rewriteUserMessage(
        conversationId: ConversationId,
        messageId: MessageId,
        replacementText: String
    ): Conversation {
        val current = requireNotNull(find(conversationId)) {
            "Unknown conversation"
        }

        val updated = current.rewriteUserMessage(
            messageId = messageId,
            replacementText = replacementText
        )

        save(updated)

        return requireNotNull(find(conversationId)) {
            "Conversation disappeared after rewrite"
        }
    }
}