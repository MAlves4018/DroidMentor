package data.remote.gemini

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.defaultRequest

/**
 * Creates the Ktor HTTP client used to communicate
 * with the Gemini REST API.
 *
 * The client is configured with:
 * - Kotlinx Serialization for JSON.
 * - Request, connection, and socket timeouts.
 * - JSON content negotiation.
 * - Explicit HTTP error handling.
 */
object GeminiHttpClientFactory {

    private const val REQUEST_TIMEOUT = 60_000L
    private const val CONNECT_TIMEOUT = 15_000L
    private const val SOCKET_TIMEOUT = 60_000L

    /**
     * Creates a configured Ktor HttpClient.
     *
     * Production uses the OkHttp engine.
     * Tests can inject a MockEngine.
     */
    fun create(
        engine: HttpClientEngine? = null
    ): HttpClient {

        val jsonConfig = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
        }

        val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {

            expectSuccess = false

            install(ContentNegotiation) {
                json(jsonConfig)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT
                connectTimeoutMillis = CONNECT_TIMEOUT
                socketTimeoutMillis = SOCKET_TIMEOUT
            }

            defaultRequest {
                headers.append(
                    HttpHeaders.Accept,
                    ContentType.Application.Json.toString()
                )
            }
        }

        return if (engine != null) {
            HttpClient(engine, configure)
        } else {
            HttpClient(OkHttp, configure)
        }
    }
}
