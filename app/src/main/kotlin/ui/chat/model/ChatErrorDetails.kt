package ui.chat.model

/** Safe diagnostic metadata only; never store API keys, prompts or response bodies. */
data class ChatErrorDetails(
    val httpStatus: Int? = null,
    val apiStatus: String? = null,
    val reason: String? = null,
)
