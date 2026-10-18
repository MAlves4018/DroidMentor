package ui.historyChat

import ui.historyChat.components.ConversationGroup

/** Immutable presentation state. Empty, loading and failure can be previewed independently. */
data class HistoryUiState(
    val groups: List<ConversationGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingDeletion: ConversationSummary? = null,
)
