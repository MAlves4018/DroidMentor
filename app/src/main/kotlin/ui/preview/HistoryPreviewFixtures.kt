package ui.historyChat.preview

import ui.historyChat.ConversationSummary
import ui.historyChat.HistoryUiState
import ui.historyChat.components.ConversationGroup

/** Pure, deterministic preview snapshots. No repository, Android context or network dependency. */
object HistoryPreviewFixtures {
    val populated = HistoryUiState(
        groups = listOf(
            ConversationGroup(
                title = "Today",
                conversations = listOf(
                    ConversationSummary("1", "Jetpack Compose Navigation", "Today, 12:34"),
                    ConversationSummary("2", "Understanding Kotlin Coroutines", "Today, 10:15"),
                ),
            ),
            ConversationGroup(
                title = "Yesterday",
                conversations = listOf(
                    ConversationSummary("3", "Room Database Architecture", "Yesterday, 18:20"),
                ),
            ),
            ConversationGroup(
                title = "Previous 7 Days",
                conversations = listOf(
                    ConversationSummary("4", "Ktor API Requests", "October 5, 21:10"),
                ),
            ),
        ),
    )
    val empty = HistoryUiState()
    val loading = HistoryUiState(isLoading = true)
    val error = HistoryUiState(errorMessage = "Unable to load conversations.")
}
