package data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert


@Dao
interface MessageDao {

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