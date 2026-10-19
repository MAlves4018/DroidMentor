package domain.provider

import domain.model.Message

interface MentorProvider {

    suspend fun generateReply(
        history: List<Message>,
        apiKey: String
    ): String
}