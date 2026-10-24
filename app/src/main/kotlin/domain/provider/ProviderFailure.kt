package domain.provider

/**
 * The transport layer converts HTTP errors, network exceptions,
 * and invalid responses into one of these failure types.
 *
 * CancellationException must never be converted into
 * ProviderFailure.
 */
sealed class ProviderFailure(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * The device has no available network connection.
     */
    data object Offline : ProviderFailure(
        "No network connection available."
    )

    /**
     * The API key is invalid, missing, or lacks permission.
     * Typically HTTP 401 or 403.
     */
    data object Authentication : ProviderFailure(
        "Invalid API key or insufficient permissions."
    )

    /**
     * The provider rejected the request due to rate limits.
     * HTTP 429.
     *
     * retryAfter preserves the optional Retry-After header.
     * It may contain a delay in seconds (e.g. "60")
     * or an HTTP date.
     *
     * null means the server did not provide the header.
     */
    data class RateLimited(
        val retryAfter: String? = null
    ) : ProviderFailure(
        "Too many requests. Please try again later."
    )

    /**
     * The provider encountered a server-side error.
     * HTTP 500-599.
     */
    data class ServerError(
        val statusCode: Int
    ) : ProviderFailure(
        "Provider server error (HTTP $statusCode)."
    )

    /**
     * The request exceeded the configured timeout.
     */
    data object Timeout : ProviderFailure(
        "The request timed out."
    )

    /**
     * A network transport error occurred.
     * Examples: DNS failure, connection reset,
     * or inability to establish a connection.
     */
    class Transport(
        cause: Throwable
    ) : ProviderFailure(
        "Unable to communicate with the provider.",
        cause
    )

    /**
     * The provider returned an invalid or unexpected response.
     * Examples: malformed JSON, missing candidates,
     * or a response without usable text.
     */
    class Protocol(
        cause: Throwable? = null
    ) : ProviderFailure(
        "Invalid response received from the provider.",
        cause
    )

    /**
     * An HTTP error not covered by the specific categories.
     * Examples: HTTP 400, 404, or 422.
     */
    data class HttpError(
        val statusCode: Int
    ) : ProviderFailure(
        "Provider request failed (HTTP $statusCode)."
    )
}
