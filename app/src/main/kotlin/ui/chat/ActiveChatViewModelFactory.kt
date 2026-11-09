package ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import domain.model.identity.ConversationId
import domain.provider.MentorProvider
import domain.repository.ConversationRepository
import domain.repository.SettingsRepository
import domain.usecase.SendMessageUseCase

/** Constructor injection without Hilt; no second conversation repository. */
class ActiveChatViewModelFactory(
    private val conversationId: ConversationId,
    private val repository: ConversationRepository,
    private val provider: MentorProvider,
    private val settings: SettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ActiveChatViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ActiveChatViewModel(
            conversationId = conversationId,
            repository = repository,
            settingsRepository = settings,
            sendMessageUseCase = SendMessageUseCase(provider, settings),
        ) as T
    }
}
