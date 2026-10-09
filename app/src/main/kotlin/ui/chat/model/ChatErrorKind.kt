package ui.chat.model

enum class ChatErrorKind {
    INVALID_API_KEY,
    RATE_LIMITED,
    SERVER_UNAVAILABLE,
    TIMEOUT,
    TRANSPORT,
    PROTOCOL,
    GENERIC_PROVIDER_ERROR,
}
