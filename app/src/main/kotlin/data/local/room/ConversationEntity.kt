package data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["updatedAtMillis"]),
        Index(value = ["isActive"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,

    val createdAtMillis: Long,

    val updatedAtMillis: Long,

    val isActive: Boolean = false
)

