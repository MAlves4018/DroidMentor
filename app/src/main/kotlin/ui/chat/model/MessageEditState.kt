package ui.chat.model

sealed interface MessageEditState {
    data object None : MessageEditState

    data class Editing(
        val messageId: String,
        val draft: String,
    ) : MessageEditState

    data class ConfirmRewrite(
        val messageId: String,
        val replacementText: String,
    ) : MessageEditState
}
