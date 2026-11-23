package data.remote.gemini

import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import domain.provider.GeminiApiErrorCategory
import domain.provider.ProviderFailure
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import platform.connectivity.ConnectivityChecker
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class GeminiErrorIntegrationTest {
    private val history = listOf(
        Message(MessageId("one"), Role.USER, "Hi", 0L)
    )

    @Test
    fun `HTTP 400 with API_KEY_INVALID is not a generic provider error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":{"code":400,"status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = GeminiHttpClientFactory.create(engine)
        try {
            val provider = GeminiMentorProvider(client, ConnectivityChecker { true })
            val failure = try {
                provider.generateReply(history, "test-key")
                throw AssertionError("Should reject invalid key")
            } catch (error: ProviderFailure.GeminiApiError) {
                error
            }
            assertEquals(GeminiApiErrorCategory.INVALID_API_KEY, failure.category)
            assertEquals(400, failure.httpStatus)
        } finally {
            client.close()
        }
    }

    @Test
    fun `internet lost after preflight turns IOException into Offline`() = runTest {
        val online = AtomicBoolean(true)
        val engine = MockEngine {
            online.set(false)
            throw IOException("Simulated connection drop")
        }
        val client = GeminiHttpClientFactory.create(engine)
        try {
            val provider = GeminiMentorProvider(client, ConnectivityChecker { online.get() })
            val failure = try {
                provider.generateReply(history, "test-key")
                throw AssertionError("Expected connection failure")
            } catch (error: ProviderFailure) {
                error
            }
            assertTrue(failure is ProviderFailure.Offline)
        } finally {
            client.close()
        }
    }
}
