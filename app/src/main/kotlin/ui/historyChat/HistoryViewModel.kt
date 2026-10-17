package ui.historyChat

import androidx.lifecycle.ViewModel
import domain.model.identity.ConversationId
import domain.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ui.historyChat.components.ConversationGroup
import ui.navigation.NavigationAction

/** Owns History presentation behavior. The repository remains the only conversation store. */
class HistoryViewModel(private val repository: ConversationRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Called by the host when the current in-memory repository snapshot changes. */
    fun refresh() {
        try {
            val conversations = repository.conversations.toList()
            val activeId = repository.activeId
            val summaries = conversations.map { conversation ->
                ConversationSummary(
                    id = conversation.id.value,
                    title = conversation.messages.firstOrNull()?.content?.take(40)
                        ?: "New conversation",
                    lastActivity = if (activeId == conversation.id) "Active" else "Saved",
                )
                        }
            _uiState.value = HistoryUiState(
                groups = listOf(ConversationGroup("Conversations", summaries)),
                pendingDeletion = _uiState.value.pendingDeletion?.let { pending ->
                    summaries.firstOrNull { it.id == pending.id }
                },
            )
        } catch (_: Exception) {
            _uiState.value = HistoryUiState(errorMessage = "Unable to load conversations.")
        }
    }

    fun onAction(action: ChatHistoryAction): HistoryEffect? {
        return when (action) {
            ChatHistoryAction.NewConversation ->
                HistoryEffect.Navigate(NavigationAction.NewChat)

            ChatHistoryAction.OpenSettings ->
                HistoryEffect.Navigate(NavigationAction.OpenSettings)

            ChatHistoryAction.OpenAbout ->
                HistoryEffect.Navigate(NavigationAction.OpenAbout)

            ChatHistoryAction.OpenTitle ->
                HistoryEffect.Navigate(NavigationAction.OpenTitle)

            is ChatHistoryAction.OpenConversation -> {
                try {
                    val id = ConversationId(action.conversationId)
                    if (repository.find(id) != null) {
                        HistoryEffect.Navigate(NavigationAction.OpenConversation(id))
                    } else {
                        refresh()
                        null
                    }
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Unable to open conversation.")
                    null
                }
            }

            is ChatHistoryAction.RequestDeleteConversation -> {
                try {
                    val id = ConversationId(action.conversationId)
                    val conversation = repository.find(id)
                    if (conversation == null) {
                        _uiState.value = _uiState.value.copy(pendingDeletion = null)
                        refresh()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            pendingDeletion = ConversationSummary(
                                id = conversation.id.value,
                                title = conversation.messages.firstOrNull()?.content?.take(40)
                                    ?: "New conversation",
                                lastActivity = "",
                            ),
                        )
                    }
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(
                        pendingDeletion = null,
                        errorMessage = "Unable to delete conversation.",
                    )
                }
                null
            }

            ChatHistoryAction.CancelDeleteConversation -> {
                _uiState.value = _uiState.value.copy(pendingDeletion = null)
                null
            }

            ChatHistoryAction.ConfirmDeleteConversation -> {
                val pending = _uiState.value.pendingDeletion ?: return null
                try {
                    val id = ConversationId(pending.id)
                    if (repository.find(id) == null) {
                        _uiState.value = _uiState.value.copy(pendingDeletion = null)
                        refresh()
                        null
                    } else {
                        repository.delete(id)
                        _uiState.value = _uiState.value.copy(pendingDeletion = null)
                        refresh()
                        HistoryEffect.ConversationDeleted(id)
                    }
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Unable to delete conversation.")
                    null
                }
            }
        }
    }
}
