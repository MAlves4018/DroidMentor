package ui.historyChat

sealed interface ChatHistoryAction {
    data class OpenConversation(val conversationId: String) : ChatHistoryAction
    data class RequestDeleteConversation(val conversationId: String) : ChatHistoryAction
    data object ConfirmDeleteConversation : ChatHistoryAction
    data object CancelDeleteConversation : ChatHistoryAction
    data object NewConversation : ChatHistoryAction
    data object OpenSettings : ChatHistoryAction
    data object OpenAbout : ChatHistoryAction
    data object OpenTitle : ChatHistoryAction
}
