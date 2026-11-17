package data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MessageDao {

    // Check message ownership in batches for older SQLite versions.
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

    // Retrieve the exact persisted message.
    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        AND id = :messageId
        LIMIT 1
    """)
    suspend fun findById(
        conversationId: String,
        messageId: String
    ): MessageEntity?

    // Update only an existing USER message.
    @Query("""
        UPDATE messages
        SET content = :newContent
        WHERE conversationId = :conversationId
        AND id = :messageId
        AND role = 'USER'
    """)
    suspend fun updateUserContent(
        conversationId: String,
        messageId: String,
        newContent: String
    ): Int

    @Upsert
    suspend fun upsertAll(
        messages: List<MessageEntity>
    )

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
