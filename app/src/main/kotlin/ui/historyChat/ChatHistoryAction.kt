package ui.historyChat

sealed interface ChatHistoryAction {

    data class OpenConversation(
        val conversationId: String
    ) : ChatHistoryAction

    data class DeleteConversation(
        val conversationId: String
    ) : ChatHistoryAction

    data object NewConversation : ChatHistoryAction
}