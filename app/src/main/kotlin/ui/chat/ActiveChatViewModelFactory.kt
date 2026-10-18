package ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository

/** Constructor injection without introducing Hilt or a second repository. */
class ActiveChatViewModelFactory(
    private val conversationId: ConversationId,
    private val repository: ConversationRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ActiveChatViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ActiveChatViewModel(conversationId, repository) as T
    }
}
