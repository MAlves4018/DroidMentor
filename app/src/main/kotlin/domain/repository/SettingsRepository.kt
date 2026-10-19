package domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val hasApiKey: Flow<Boolean>

    suspend fun getApiKey(): String?

    suspend fun saveApiKey(value: String)

    suspend fun removeApiKey()
}