package ui.historyChat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import domain.repository.ConversationRepository

/** Explicit constructor injection; ARCH-001 may later supply the same interface. */
class HistoryViewModelFactory(
    private val repository: ConversationRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(repository) as T
    }
}
