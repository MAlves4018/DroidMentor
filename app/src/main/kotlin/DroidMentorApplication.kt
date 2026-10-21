package isel.dei.pdm.droidmentor

import android.app.Application

import data.InMemorySettingsRepository
import data.remote.gemini.GeminiHttpClientFactory
import data.remote.gemini.GeminiMentorProvider
import domain.provider.MentorProvider
import domain.repository.ConversationRepository
import domain.repository.InMemoryConversationRepository
import domain.repository.SettingsRepository
import platform.connectivity.AndroidConnectivityChecker
import platform.connectivity.ConnectivityChecker

/**
 * Application-level service locator.
 */
class DroidMentorApplication : Application() {

    /**
     * Repository responsible for conversation data.
     * Currently uses the existing in-memory implementation.
     */
    val conversations: ConversationRepository by lazy {
        InMemoryConversationRepository()
    }

    /**
     * Repository responsible for application settings.
     * Currently uses the existing in-memory implementation.
     */
    val settings: SettingsRepository by lazy {
        InMemorySettingsRepository()
    }

    /**
     * Checks the device's network connectivity.
     */
    val connectivityChecker: ConnectivityChecker by lazy {
        AndroidConnectivityChecker(this)
    }

    /**
     * Shared Ktor HTTP client.
     * Created once and reused across Gemini requests.
     */
    private val geminiHttpClient by lazy {
        GeminiHttpClientFactory.create()
    }

    /**
     * Real Gemini implementation of MentorProvider.
     * The rest of the application depends only on
     * the domain interface, not the Gemini implementation.
     */
    val mentor: MentorProvider by lazy {
        GeminiMentorProvider(
            client = geminiHttpClient,
            connectivityChecker = connectivityChecker
        )
    }
}
