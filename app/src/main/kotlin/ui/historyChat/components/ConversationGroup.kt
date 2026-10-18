package ui.historyChat.components

import ui.historyChat.ConversationSummary

/** Presentational grouping independent of Compose rendering. */
data class ConversationGroup(
    val title: String,
    val conversations: List<ConversationSummary>,
)
