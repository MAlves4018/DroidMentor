package data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MessageDao {

    // Caller must bound messageIds to 998 on legacy Android (one extra parent bind).
    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE id IN (:messageIds) AND conversationId != :conversationId
    """)
    suspend fun countOwnedByOtherConversation(
        conversationId: String,
        messageIds: List<String>
    ): Int

    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY sequence ASC
    """)
    suspend fun findByConversationId(
        conversationId: String
    ): List<MessageEntity>

    @Upsert
    suspend fun upsertAll(
        messages: List<MessageEntity>
    )

    // Caller must bound messageIds to 998; NOT IN batches are not equivalent deletes.
    @Query("""
        DELETE FROM messages
        WHERE conversationId = :conversationId
        AND id NOT IN (:messageIds)
    """)
    suspend fun deleteExcept(
        conversationId: String,
        messageIds: List<String>
    ): Int

    @Query("""
        DELETE FROM messages
        WHERE conversationId = :conversationId
        AND sequence > :sequence
    """)
    suspend fun deleteAfterSequence(
        conversationId: String,
        sequence: Long
    ): Int
}
