package ui.chat.model

sealed interface ChatRequestState {
    data object Idle : ChatRequestState
    data object Generating : ChatRequestState
    data object Stopped : ChatRequestState

    data class Error(
        val kind: ChatErrorKind,
    ) : ChatRequestState
}
