package data

import domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemorySettingsRepository : SettingsRepository {

    private val apiKey = MutableStateFlow<String?>(null)

    override val hasApiKey: Flow<Boolean> =
        apiKey.map { !it.isNullOrBlank() }

    override suspend fun getApiKey(): String? =
        apiKey.value

    override suspend fun saveApiKey(value: String) {
        require(value.isNotBlank()) {
            "API key cannot be blank"
        }

        apiKey.value = value
    }

    override suspend fun removeApiKey() {
        apiKey.value = null
    }
}