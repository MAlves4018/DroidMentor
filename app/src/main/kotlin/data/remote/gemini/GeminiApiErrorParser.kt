package data.remote.gemini

import domain.provider.ProviderFailure
import domain.provider.GeminiApiErrorCategory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Classifies unsuccessful Gemini generateContent HTTP replies.
 * Reads structured Google error fields. Raw messages are never exposed to the UI/logs.
 */
internal object GeminiApiErrorParser {
    private val safeCode = Regex("[A-Z0-9_]{1,80}")

    fun classify(statusCode: Int, responseBody: String, retryAfter: String?): ProviderFailure {
        val root = try {
            Json.parseToJsonElement(responseBody) as? JsonObject
        } catch (_: Exception) {
            null
        }
        val error = root?.get("error") as? JsonObject
        val apiStatus = codeOf(error?.get("status"))
        val details = (error?.get("details") as? JsonArray).orEmpty()
        val reason = details.firstNotNullOfOrNull { element ->
            codeOf((element as? JsonObject)?.get("reason"))
        }
        val hasQuotaFailure = details.any { detail ->
            val type = ((detail as? JsonObject)?.get("@type") as? JsonPrimitive)?.content
            type == "type.googleapis.com/google.rpc.QuotaFailure"
        }

        // This happens with HTTP 400 INVALID_ARGUMENT in real Gemini responses.
        if (reason == "API_KEY_INVALID" || reason == "API_KEY_EXPIRED") {
            return apiError(GeminiApiErrorCategory.INVALID_API_KEY, statusCode, apiStatus, reason)
        }
        if (reason in setOf("BILLING_DISABLED", "BILLING_NOT_ENABLED")) {
            return apiError(GeminiApiErrorCategory.BILLING_REQUIRED, statusCode, apiStatus, reason)
        }

        return when (statusCode) {
            400, 422 -> apiError(GeminiApiErrorCategory.INVALID_REQUEST, statusCode, apiStatus, reason)
            401 -> ProviderFailure.Authentication
            402 -> apiError(GeminiApiErrorCategory.BILLING_REQUIRED, statusCode, apiStatus, reason)
            403 -> apiError(GeminiApiErrorCategory.PERMISSION_DENIED, statusCode, apiStatus, reason)
            404 -> apiError(GeminiApiErrorCategory.MODEL_UNAVAILABLE, statusCode, apiStatus, reason)
            408, 504 -> ProviderFailure.Timeout
            429 -> if (hasQuotaFailure || reason == "QUOTA_EXCEEDED") {
                apiError(GeminiApiErrorCategory.QUOTA_EXCEEDED, statusCode, apiStatus, reason)
            } else {
                ProviderFailure.RateLimited(retryAfter)
            }
            in 500..599 -> ProviderFailure.ServerError(statusCode)
            else -> ProviderFailure.HttpError(statusCode)
        }
    }

    private fun codeOf(element: kotlinx.serialization.json.JsonElement?): String? {
        val value = (element as? JsonPrimitive)?.content ?: return null
        return value.takeIf(safeCode::matches)
    }

    private fun apiError(
        category: GeminiApiErrorCategory,
        httpStatus: Int,
        apiStatus: String?,
        reason: String?,
    ) = ProviderFailure.GeminiApiError(category, httpStatus, apiStatus, reason)
}
