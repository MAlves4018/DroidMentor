package isel.dei.pdm.droidmentor

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import data.remote.gemini.GeminiHttpClientFactory
import data.remote.gemini.GeminiMentorProvider
import data.settings.DataStoreSettingsRepository
import domain.provider.MentorProvider
import domain.repository.ConversationRepository
import androidx.room.Room
import data.local.room.DroidMentorDatabase
import data.repository.RoomConversationRepository
import domain.repository.SettingsRepository
import platform.connectivity.AndroidConnectivityChecker
import platform.connectivity.ConnectivityChecker
import java.io.File

/** Application-level service locator. */
class DroidMentorApplication : Application() {

    private val database: DroidMentorDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            DroidMentorDatabase::class.java,
            DroidMentorDatabase.DATABASE_NAME
        ).build()
    }

    val conversations: ConversationRepository by lazy {
        RoomConversationRepository(database)
    }

    // Single DataStore per file. noBackupFilesDir excludes the key from Android backups.
    private val keyStore by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { File(noBackupFilesDir, "droidmentor_gemini.preferences_pb") },
        )
    }

    val settings: SettingsRepository by lazy {
        DataStoreSettingsRepository(keyStore)
    }

    val connectivityChecker: ConnectivityChecker by lazy {
        AndroidConnectivityChecker(this)
    }

    private val geminiHttpClient by lazy {
        GeminiHttpClientFactory.create()
    }

    val mentor: MentorProvider by lazy {
        GeminiMentorProvider(
            client = geminiHttpClient,
            connectivityChecker = connectivityChecker,
        )
    }
}
