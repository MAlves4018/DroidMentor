package data.remote.gemini

import domain.model.Message
import domain.model.Role
import domain.model.identity.MessageId
import domain.provider.ProviderFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import platform.connectivity.ConnectivityChecker
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CompletableDeferred

class GeminiMentorProviderTest {

    private val testApiKey = "test-api-key"

    private val history = listOf(
        Message(
            id = MessageId("1"),
            role = Role.USER,
            content = "What is a ViewModel?",
            sequence = 0L
        ),
        Message(
            id = MessageId("2"),
            role = Role.MODEL,
            content = "A ViewModel manages UI state.",
            sequence = 1L
        ),
        Message(
            id = MessageId("3"),
            role = Role.USER,
            content = "Can you explain StateFlow?",
            sequence = 2L
        )
    )

    private val jsonHeaders = headersOf(
        HttpHeaders.ContentType,
        "application/json"
    )

    private fun createProvider(
        engine: MockEngine,
        connected: Boolean = true
    ): Pair<GeminiMentorProvider, HttpClient> {

        val client = GeminiHttpClientFactory.create(engine)

        val provider = GeminiMentorProvider(
            client = client,
            connectivityChecker = ConnectivityChecker {
                connected
            }
        )

        return provider to client
    }

    private suspend inline fun <reified T : Throwable> expectFailure(
        crossinline block: suspend () -> Unit
    ): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw AssertionError(
                "Expected ${T::class.simpleName}, got ${e::class.simpleName}",
                e
            )
        }

        throw AssertionError(
            "Expected ${T::class.simpleName} but no exception was thrown"
        )
    }

    @Test
    fun `successful response returns generated text`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "role": "model",
                            "parts": [
                              {"text": "StateFlow is a state holder."}
                            ]
                          }
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            val result = provider.generateReply(
                history,
                testApiKey
            )

            assertEquals(
                "StateFlow is a state holder.",
                result
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun `request contains API key and complete history`() = runTest {
        var requestReceived = false

        val engine = MockEngine { request ->
            requestReceived = true

            assertEquals(
                "test-api-key",
                request.headers["x-goog-api-key"]
            )

            assertEquals(
                "POST",
                request.method.value
            )

            assertTrue(
                request.url.toString().contains(
                    "generateContent"
                )
            )

            val body = request.body.toByteArray()
                .decodeToString()

            assertTrue(body.contains("system_instruction"))
            assertTrue(body.contains("What is a ViewModel?"))
            assertTrue(body.contains("A ViewModel manages UI state."))
            assertTrue(body.contains("Can you explain StateFlow?"))
            assertTrue(body.contains("\"role\":\"user\""))
            assertTrue(body.contains("\"role\":\"model\""))

            respond(
                content = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [{"text": "Test reply"}]
                          }
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            provider.generateReply(history, testApiKey)
            assertTrue(requestReceived)
        } finally {
            client.close()
        }
    }

    @Test
    fun `offline prevents HTTP request`() = runTest {
        var requestReceived = false

        val engine = MockEngine {
            requestReceived = true
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }

        val (provider, client) = createProvider(
            engine = engine,
            connected = false
        )

        try {
            expectFailure<ProviderFailure.Offline> {
                provider.generateReply(history, testApiKey)
            }

            assertFalse(requestReceived)
        } finally {
            client.close()
        }
    }

    @Test
    fun `blank API key prevents HTTP request`() = runTest {
        var requestReceived = false

        val engine = MockEngine {
            requestReceived = true
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }

        val (provider, client) = createProvider(engine)

        try {
            expectFailure<ProviderFailure.Authentication> {
                provider.generateReply(history, " ")
            }

            assertFalse(requestReceived)
        } finally {
            client.close()
        }
    }

    @Test
    fun `HTTP 401 maps to Authentication`() = runTest {
        assertHttpFailure<ProviderFailure.Authentication>(
            HttpStatusCode.Unauthorized
        )
    }

    @Test
    fun `HTTP 403 maps to Authentication`() = runTest {
        assertHttpFailure<ProviderFailure.Authentication>(
            HttpStatusCode.Forbidden
        )
    }


    @Test
    fun `HTTP 429 without Retry-After maps to RateLimited`() = runTest {
        val failure = assertHttpFailure<ProviderFailure.RateLimited>(
            HttpStatusCode.TooManyRequests
        )

        assertEquals(null, failure.retryAfter)
    }

    @Test
    fun `HTTP 429 preserves Retry-After seconds`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":{"code":429,"message":"Rate limit exceeded"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(
                    HttpHeaders.RetryAfter,
                    "60"
                )
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            val failure = expectFailure<ProviderFailure.RateLimited> {
                provider.generateReply(history, testApiKey)
            }

            assertEquals("60", failure.retryAfter)
        } finally {
            client.close()
        }
    }

    @Test
    fun `HTTP 429 preserves Retry-After HTTP date`() = runTest {
        val retryDate = "Wed, 28 Oct 2026 12:00:00 GMT"

        val engine = MockEngine {
            respond(
                content = """{"error":{"code":429,"message":"Rate limit exceeded"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(
                    HttpHeaders.RetryAfter,
                    retryDate
                )
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            val failure = expectFailure<ProviderFailure.RateLimited> {
                provider.generateReply(history, testApiKey)
            }

            assertEquals(retryDate, failure.retryAfter)
        } finally {
            client.close()
        }
    }


    @Test
    fun `HTTP 500 maps to ServerError`() = runTest {
        val failure = assertHttpFailure<ProviderFailure.ServerError>(
            HttpStatusCode.InternalServerError
        )

        assertEquals(500, failure.statusCode)
    }

    @Test
    fun `HTTP 503 maps to ServerError`() = runTest {
        val failure = assertHttpFailure<ProviderFailure.ServerError>(
            HttpStatusCode.ServiceUnavailable
        )

        assertEquals(503, failure.statusCode)
    }

    @Test
    fun `HTTP 400 maps to HttpError`() = runTest {
        val failure = assertHttpFailure<ProviderFailure.HttpError>(
            HttpStatusCode.BadRequest
        )

        assertEquals(400, failure.statusCode)
    }

    private suspend inline fun <reified T : ProviderFailure> assertHttpFailure(
        status: HttpStatusCode
    ): T {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "error": {
                        "code": ${status.value},
                        "message": "Simulated API error",
                        "status": "ERROR"
                      }
                    }
                """.trimIndent(),
                status = status,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        return try {
            expectFailure<T> {
                provider.generateReply(history, testApiKey)
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun `malformed JSON maps to Protocol`() = runTest {
        val engine = MockEngine {
            respond(
                content = "{invalid-json",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            expectFailure<ProviderFailure.Protocol> {
                provider.generateReply(history, testApiKey)
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun `missing candidates maps to Protocol`() = runTest {
        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            expectFailure<ProviderFailure.Protocol> {
                provider.generateReply(history, testApiKey)
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun `empty response text maps to Protocol`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [{"text": "   "}]
                          }
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            expectFailure<ProviderFailure.Protocol> {
                provider.generateReply(history, testApiKey)
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun `multiple text parts are combined`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {"text": "Hello "},
                              {"text": "Android!"}
                            ]
                          }
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            val result = provider.generateReply(
                history,
                testApiKey
            )

            assertEquals("Hello Android!", result)
        } finally {
            client.close()
        }
    }

    @Test
    fun `transport IOException maps to Transport`() = runTest {
        val engine = MockEngine {
            throw IOException("Simulated connection failure")
        }

        val (provider, client) = createProvider(engine)

        try {
            expectFailure<ProviderFailure.Transport> {
                provider.generateReply(history, testApiKey)
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun `socket timeout maps to Timeout`() = runTest {
        val engine = MockEngine {
            throw SocketTimeoutException("Simulated timeout")
        }

        val (provider, client) = createProvider(engine)

        try {
            expectFailure<ProviderFailure.Timeout> {
                provider.generateReply(history, testApiKey)
            }
        } finally {
            client.close()
        }
    }


    @Test
    fun `coroutine cancellation is preserved after HTTP request starts`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()

        val engine = MockEngine {
            // Signal that the HTTP request reached MockEngine.
            requestStarted.complete(Unit)

            // Simulate a slow API response.
            // Cancellation should interrupt this suspension.
            delay(60_000)

            respond(
                content = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "Late reply"}]
                      }
                    }
                  ]
                }
            """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders
            )
        }

        val (provider, client) = createProvider(engine)

        try {
            val job = launch {
                provider.generateReply(history, testApiKey)
            }

            // Do not cancel until MockEngine confirms
            // that the HTTP request has started.
            requestStarted.await()

            // Cancel the active request and wait for termination.
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
        } finally {
            client.close()
        }
    }
}