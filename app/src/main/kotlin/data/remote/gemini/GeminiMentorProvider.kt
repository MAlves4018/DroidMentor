
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
 * - Interpret Gemini generation results.
 * - Convert failures into ProviderFailure.
 * - Preserve coroutine cancellation.
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

        private val BLOCKED_FINISH_REASONS = setOf(
            "SAFETY",
            "RECITATION",
            "BLOCKLIST",
            "PROHIBITED_CONTENT",
            "SPII",
            "IMAGE_SAFETY",
            "MODEL_ARMOR"
        )
    }

    /**
     * Generates a mentor reply using the supplied
     * local conversation history and BYOK key.
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
            throw e

        } catch (e: ProviderFailure) {
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
            throw ProviderFailure.Protocol(e)
        }
    }

    /**
     * Interprets Gemini's response.
     *
     * Blocked and truncated responses are valid API
     * outcomes, not malformed JSON.
     *
     * Only a completed response with usable text
     * is returned as a successful mentor reply.
     */
    private fun extractText(
        response: GeminiGenerateContentResponse
    ): String {

        // Gemini may reject the prompt before
        // generating any candidates.
        val promptBlockReason =
            response.promptFeedback?.blockReason

        if (!promptBlockReason.isNullOrBlank()) {
            throw ProviderFailure.ContentBlocked(
                reason = promptBlockReason
            )
        }

        val candidate = response.candidates.firstOrNull()
            ?: throw ProviderFailure.Protocol()

        val text = candidate.content
            ?.parts
            ?.joinToString(separator = "") { it.text }
            ?.trim()

        // Check the finish reason before returning text.
        // A truncated or blocked response may still
        // contain partial generated content.
        when (val finishReason = candidate.finishReason) {
            "MAX_TOKENS" -> {
                throw ProviderFailure.ResponseTruncated(
                    partialText = text?.takeIf { it.isNotBlank() }
                )
            }

            in BLOCKED_FINISH_REASONS -> {
                throw ProviderFailure.ContentBlocked(
                    reason = finishReason
                )
            }

            "STOP", null -> {
                // Continue with normal text validation.
            }

            else -> {
                // Unknown or unsuccessful finish reason.
                // Do not silently treat it as success.
                throw ProviderFailure.Protocol()
            }
        }

        if (text.isNullOrBlank()) {
            throw ProviderFailure.Protocol()
        }

        return text
    }
}
