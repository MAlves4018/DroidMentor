package domain.provider

/**
 * Provider-independent events describing the generation
 * of a mentor response.
 *
 * The UI can consume these events without knowing
 * whether the provider uses a single HTTP response
 * or streaming.
 */
sealed interface MentorResponse {

    /**
     * The current generated text.
     *
     * This is a complete snapshot of the text generated
     * so far.
     *
     * Future streaming implementations can emit
     * multiple ContentUpdated events.
     */
    data class ContentUpdated(
        val text: String
    ) : MentorResponse

    /**
     * Generation has completed successfully.
     *
     * For the current non-streaming implementation,
     * this contains the final Gemini response.
     */
    data class Completed(
        val text: String
    ) : MentorResponse
}
