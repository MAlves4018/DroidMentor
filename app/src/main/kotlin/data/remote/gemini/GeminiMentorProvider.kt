package data.remote.gemini

import domain.model.Message
import domain.provider.MentorProvider
import domain.provider.ProviderFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import platform.connectivity.ConnectivityChecker
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Gemini REST implementation of MentorProvider.
 *
 * Responsibilities:
 * - Check connectivity before sending requests.
 * - Convert conversation history into Gemini DTOs.
 * - Send requests through Ktor.
 * - Extract generated text from successful responses.
 * - Convert failures into ProviderFailure.
 * - Preserve coroutine cancellation.
 *
 */
class GeminiMentorProvider(
    private val client: HttpClient,
    private val connectivityChecker: ConnectivityChecker,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = DEFAULT_BASE_URL
) : MentorProvider {

    companion object {
        private const val DEFAULT_MODEL = "gemini-2.5-flash"

        private const val DEFAULT_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta"
    }

    /**
     * Generates a mentor reply using the complete
     * conversation history.
     *
     * @throws ProviderFailure when the request fails.
     * @throws CancellationException when cancelled.
     */
    override suspend fun generateReply(
        history: List<Message>,
        apiKey: String
    ): String {

        if (apiKey.isBlank()) {
            throw ProviderFailure.Authentication
        }

        if (!connectivityChecker.isConnected()) {
            throw ProviderFailure.Offline
        }

        val request = GeminiMessageMapper.toRequest(history)

        return try {
            val response = client.post(
                "${baseUrl.trimEnd('/')}/models/$model:generateContent"
            ) {
                contentType(ContentType.Application.Json)

                header("x-goog-api-key", apiKey)

                setBody(request)
            }

            when (response.status.value) {
                in 200..299 -> {
                    val result =
                        response.body<GeminiGenerateContentResponse>()

                    extractText(result)
                }

                401, 403 -> {
                    throw ProviderFailure.Authentication
                }

                429 -> {
                    throw ProviderFailure.RateLimited(
                        retryAfter = response.headers[HttpHeaders.RetryAfter]
                    )
                }

                in 500..599 -> {
                    throw ProviderFailure.ServerError(
                        response.status.value
                    )
                }

                else -> {
                    throw ProviderFailure.HttpError(
                        response.status.value
                    )
                }
            }

        } catch (e: CancellationException) {
            // Cancellation must propagate to the caller.
            throw e

        } catch (e: ProviderFailure) {
            // Already normalized. Preserve the failure.
            throw e

        } catch (e: HttpRequestTimeoutException) {
            throw ProviderFailure.Timeout

        } catch (e: SocketTimeoutException) {
            throw ProviderFailure.Timeout

        } catch (e: JsonConvertException) {
            throw ProviderFailure.Protocol(e)

        } catch (e: SerializationException) {
            throw ProviderFailure.Protocol(e)

        } catch (e: IOException) {
            throw ProviderFailure.Transport(e)

        } catch (e: Exception) {
            // Unexpected failures are not exposed as
            // raw exceptions to the application.
            throw ProviderFailure.Protocol(e)
        }
    }

    /**
     * Extracts the generated text from Gemini's
     * first response candidate.
     *
     * A response without usable text is considered
     * a protocol failure.
     */
    private fun extractText(
        response: GeminiGenerateContentResponse
    ): String {

        val candidate = response.candidates.firstOrNull()
            ?: throw ProviderFailure.Protocol()

        val content = candidate.content
            ?: throw ProviderFailure.Protocol()

        val text = content.parts
            .map { it.text }
            .joinToString(separator = "")
            .trim()

        if (text.isBlank()) {
            throw ProviderFailure.Protocol()
        }

        return text
    }
}
