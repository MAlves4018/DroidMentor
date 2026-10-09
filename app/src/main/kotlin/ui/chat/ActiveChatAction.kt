package ui.chat

import ui.chat.model.ReasoningLevelUi

/**
 * Typed Active Chat intents. Real navigation, provider and persistence effects are attached
 * outside this W1 UI boundary in later tasks.
 */
sealed interface ActiveChatAction {
    data class DraftChanged(val value: String) : ActiveChatAction

    data object SendClicked : ActiveChatAction
    data object StopClicked : ActiveChatAction

    data object AddAttachmentClicked : ActiveChatAction
    data object RemoveAttachmentClicked : ActiveChatAction

    data class ReasoningSelected(val level: ReasoningLevelUi) : ActiveChatAction

    data class EditMessageClicked(val messageId: String) : ActiveChatAction
    data class EditDraftChanged(val value: String) : ActiveChatAction
    data object CancelEditClicked : ActiveChatAction
    data object SaveEditClicked : ActiveChatAction
    data object ReturnToEditClicked : ActiveChatAction
    data object ConfirmRewriteClicked : ActiveChatAction

    data object RetryClicked : ActiveChatAction
    data object DismissFeedbackClicked : ActiveChatAction
}
