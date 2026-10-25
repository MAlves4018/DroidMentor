package data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Local BYOK storage. The DataStore instance is provided by the application singleton. */
class DataStoreSettingsRepository(
    private val store: DataStore<Preferences>,
) : SettingsRepository {
    private companion object {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    override val hasApiKey: Flow<Boolean> = store.data
        .map { preferences -> !preferences[GEMINI_API_KEY].isNullOrBlank() }
        .distinctUntilChanged()

    override suspend fun getApiKey(): String? =
        store.data.first()[GEMINI_API_KEY]

    override suspend fun saveApiKey(value: String) {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "API key must not be blank" }
        store.edit { preferences -> preferences[GEMINI_API_KEY] = normalized }
    }

    override suspend fun removeApiKey() {
        store.edit { preferences -> preferences.remove(GEMINI_API_KEY) }
    }
}
