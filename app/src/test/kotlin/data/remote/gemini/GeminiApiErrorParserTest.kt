package data.remote.gemini

import domain.provider.GeminiApiErrorCategory
import domain.provider.ProviderFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiErrorParserTest {
    @Test
    fun `real observed HTTP 400 API_KEY_INVALID maps to key error`() {
        val result = GeminiApiErrorParser.classify(
            400,
            """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"API key not valid.","details":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"API_KEY_INVALID"}]}}""",
            null,
        )
        assertTrue(result is ProviderFailure.GeminiApiError)
        result as ProviderFailure.GeminiApiError
        assertEquals(GeminiApiErrorCategory.INVALID_API_KEY, result.category)
        assertEquals(400, result.httpStatus)
        assertEquals("INVALID_ARGUMENT", result.apiStatus)
        assertEquals("API_KEY_INVALID", result.reason)
    }

    @Test
    fun `invalid contents returns invalid request not authentication`() {
        val failure = GeminiApiErrorParser.classify(
            400,
            """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"contents is not specified"}}""",
            null,
        ) as ProviderFailure.GeminiApiError
        assertEquals(GeminiApiErrorCategory.INVALID_REQUEST, failure.category)
    }

    @Test
    fun `missing model is distinguished from other 4xx`() {
        val failure = GeminiApiErrorParser.classify(
            404,
            """{"error":{"code":404,"status":"NOT_FOUND","message":"Model not found"}}""",
            null,
        ) as ProviderFailure.GeminiApiError
        assertEquals(GeminiApiErrorCategory.MODEL_UNAVAILABLE, failure.category)
    }

    @Test
    fun `rate limited without quota details is conservatively generic`() {
        val failure = GeminiApiErrorParser.classify(429, "not json", "20")
        assertTrue(failure is ProviderFailure.RateLimited)
        assertEquals("20", (failure as ProviderFailure.RateLimited).retryAfter)
    }

    @Test
    fun `structured quota failure does not invent daily billing explanation`() {
        val failure = GeminiApiErrorParser.classify(
            429,
            """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[{"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[{"quotaId":"requests"}]}]}}""",
            "30",
        ) as ProviderFailure.GeminiApiError
        assertEquals(GeminiApiErrorCategory.QUOTA_EXCEEDED, failure.category)
        assertNull(failure.reason)
    }

    @Test
    fun `403 is not incorrectly called an invalid key`() {
        val failure = GeminiApiErrorParser.classify(403, "{}", null)
            as ProviderFailure.GeminiApiError
        assertEquals(GeminiApiErrorCategory.PERMISSION_DENIED, failure.category)
    }

    @Test
    fun `malformed error JSON still yields an HTTP classification`() {
        assertTrue(GeminiApiErrorParser.classify(503, "{{", null) is ProviderFailure.ServerError)
    }

    @Test
    fun `untrusted diagnostic reason is discarded`() {
        val failure = GeminiApiErrorParser.classify(
            400,
            """{"error":{"details":[{"reason":"secret?text\\nfoo"}]}}""",
            null,
        ) as ProviderFailure.GeminiApiError
        assertNull(failure.reason)
    }
}
