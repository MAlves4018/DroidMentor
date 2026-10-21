package data.remote.gemini

import domain.model.Message
import domain.model.Role

/**
 * Converts DroidMentor domain messages into Gemini API DTOs.
 *
 * The mapper is responsible for:
 * - Converting domain roles into Gemini roles.
 * - Preserving the chronological conversation order.
 * - Including the complete conversation history.
 * - Adding the Android mentor system instruction.
 *
 * It does not perform HTTP requests or modify domain objects.
 */
object GeminiMessageMapper {

    private const val SYSTEM_INSTRUCTION =
        "You are DroidMentor, a senior Android development " +
                "engineer and mentor. Provide accurate, clear, and " +
                "practical guidance about Android development. " +
                "Follow modern Android best practices, explain " +
                "technical concepts clearly, and provide Kotlin " +
                "examples when appropriate."

    /**
     * Converts the complete conversation history into
     * a Gemini generateContent request.
     *
     * Messages are sorted by sequence to guarantee
     * chronological order.
     */
    fun toRequest(
        history: List<Message>
    ): GeminiGenerateContentRequest {

        val contents = history
            .sortedBy { it.sequence }
            .map { message ->
                GeminiContent(
                    role = message.role.toGeminiRole(),
                    parts = listOf(
                        GeminiPart(text = message.content)
                    )
                )
            }

        return GeminiGenerateContentRequest(
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(text = SYSTEM_INSTRUCTION)
                )
            ),
            contents = contents
        )
    }

    /**
     * Maps DroidMentor roles to Gemini API roles.
     */
    private fun Role.toGeminiRole(): String =
        when (this) {
            Role.USER -> "user"
            Role.MODEL -> "model"
        }
}
