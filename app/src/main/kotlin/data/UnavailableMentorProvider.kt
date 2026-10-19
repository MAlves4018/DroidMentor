package data

import domain.model.Message
import domain.provider.MentorProvider

class UnavailableMentorProvider : MentorProvider {

    override suspend fun generateReply(
        history: List<Message>,
        apiKey: String
    ): String {
        throw IllegalStateException(
            "Gemini provider has not been configured yet"
        )
    }
}