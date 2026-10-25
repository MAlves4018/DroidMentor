package data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith

/** Uses real Preferences DataStore and a temporary file; never uses a real API key. */
@RunWith(AndroidJUnit4::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder(
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    )

    private fun repository(file: File, scope: CoroutineScope): DataStoreSettingsRepository =
        DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        )

    @Test fun saveReadReplaceRemoveWorksOnDisk() = runBlocking {
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val settings = repository(file, scope)
            assertNull(settings.getApiKey())
            assertFalse(settings.hasApiKey.first())
            settings.saveApiKey("  fake-1  ")
            assertEquals("fake-1", settings.getApiKey())
            assertTrue(settings.hasApiKey.first())
            settings.saveApiKey("fake-2")
            assertEquals("fake-2", settings.getApiKey())
            settings.removeApiKey()
            assertNull(settings.getApiKey())
            assertFalse(settings.hasApiKey.first())
            settings.removeApiKey()
            assertNull(settings.getApiKey())
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        }
    }

    @Test fun savedKeySurvivesRecreatingRepositoryAndDataStore() = runBlocking {
        val file = File(temporaryFolder.root, "restart.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            repository(file, firstScope).saveApiKey("dummy-restart-key")
        } finally {
            firstScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        }
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val restored = repository(file, secondScope)
            assertEquals("dummy-restart-key", restored.getApiKey())
            assertTrue(restored.hasApiKey.first())
            restored.removeApiKey()
            assertNull(restored.getApiKey())
        } finally {
            secondScope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        }
    }

    @Test fun blankIsRejectedWithoutWriting() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val settings = repository(File(temporaryFolder.root, "blank.preferences_pb"), scope)
            try {
                settings.saveApiKey("  ")
                org.junit.Assert.fail("Blank keys should be rejected")
            } catch (_: IllegalArgumentException) {
                // Only local non-blank validation, no provider authentication.
            }
            assertNull(settings.getApiKey())
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        }
    }
}
