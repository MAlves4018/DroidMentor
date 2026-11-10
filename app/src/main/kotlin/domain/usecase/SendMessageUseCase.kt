package domain.usecase

import domain.model.Message
import domain.provider.MentorProvider
import domain.provider.ProviderFailure
import domain.repository.SettingsRepository

/** A single stateless provider request. Conversation writes remain in the chat state holder. */
class SendMessageUseCase(
    private val provider: MentorProvider,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(history: List<Message>): String {
        val key = settings.getApiKey()?.takeIf { it.isNotBlank() }
            ?: throw ProviderFailure.Authentication
        return provider.generateReply(history, key)
    }
}
