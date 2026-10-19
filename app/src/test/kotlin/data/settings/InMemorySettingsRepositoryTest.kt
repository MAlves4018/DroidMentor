package data.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import data.InMemorySettingsRepository

class InMemorySettingsRepositoryTest {

    @Test
    fun initially_noApiKeyIsConfigured() = runBlocking {
        val repository = InMemorySettingsRepository()

        assertNull(repository.getApiKey())
        assertFalse(repository.hasApiKey.first())
    }

    @Test
    fun saveApiKey_storesKeyAndUpdatesStatus() = runBlocking {
        val repository = InMemorySettingsRepository()

        repository.saveApiKey("test-api-key")

        assertEquals("test-api-key", repository.getApiKey())
        assertTrue(repository.hasApiKey.first())
    }

    @Test
    fun saveApiKey_replacesExistingKey() = runBlocking {
        val repository = InMemorySettingsRepository()

        repository.saveApiKey("first-key")
        repository.saveApiKey("second-key")

        assertEquals("second-key", repository.getApiKey())
        assertTrue(repository.hasApiKey.first())
    }

    @Test
    fun removeApiKey_clearsStoredKey() = runBlocking {
        val repository = InMemorySettingsRepository()

        repository.saveApiKey("test-api-key")
        repository.removeApiKey()

        assertNull(repository.getApiKey())
        assertFalse(repository.hasApiKey.first())
    }

    @Test
    fun removeApiKey_whenAlreadyEmpty_isSafe() = runBlocking {
        val repository = InMemorySettingsRepository()

        repository.removeApiKey()

        assertNull(repository.getApiKey())
        assertFalse(repository.hasApiKey.first())
    }

    @Test
    fun saveApiKey_rejectsBlankKey() = runBlocking {
        val repository = InMemorySettingsRepository()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveApiKey("   ")
            }
        }

        assertNull(repository.getApiKey())
        assertFalse(repository.hasApiKey.first())
    }
}
