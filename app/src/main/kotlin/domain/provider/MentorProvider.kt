package domain.provider

import domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Domain contract for generating Android mentor responses.
 *
 * Implementations receive the ordered local conversation
 * history and the current BYOK API key.
 *
 * Provider-specific DTOs, endpoints, and serialization
 * must remain outside the domain layer.
 */
interface MentorProvider {

    /**
     * Generates a complete mentor response.
     *
     * This is the existing non-streaming contract,
     * retained for compatibility.
     *
     * Failures are reported through ProviderFailure.
     */
    suspend fun generateReply(
        history: List<Message>,
        apiKey: String
    ): String

    /**
     * Emits provider-independent response events.
     *
     * The default implementation adapts the existing
     * non-streaming method into a Flow.
     *
     * Future streaming providers can override this
     * method without changing the Active Chat contract.
     *
     * ProviderFailure exceptions propagate to the caller.
     * Coroutine cancellation must also propagate.
     */
    fun generateResponse(
        history: List<Message>,
        apiKey: String
    ): Flow<MentorResponse> = flow {
        val reply = generateReply(
            history = history,
            apiKey = apiKey
        )

        emit(
            MentorResponse.Completed(
                text = reply
            )
        )
    }
}
