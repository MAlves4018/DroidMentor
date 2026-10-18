package ui.historyChat

import domain.model.identity.ConversationId
import ui.navigation.NavigationAction

/** One-off commands returned by an action, never stored in persistent screen state. */
sealed interface HistoryEffect {
    data class Navigate(val action: NavigationAction) : HistoryEffect
    data class ConversationDeleted(val id: ConversationId) : HistoryEffect
}
