package data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("""
        SELECT * FROM conversations
        ORDER BY updatedAtMillis DESC, id ASC
    """)
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE isActive = 1 LIMIT 1")
    suspend fun findActive(): ConversationEntity?

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Query("UPDATE conversations SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActive()

    @Query("UPDATE conversations SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String): Int

    @Query("""
        UPDATE conversations
        SET updatedAtMillis = :timestamp
        WHERE id = :id
    """)
    suspend fun updateTimestamp(id: String, timestamp: Long): Int

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String): Int
}