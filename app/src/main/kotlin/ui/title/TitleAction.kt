package ui.title

import ui.chat.model.ReasoningLevelUi

sealed interface TitleAction {
    data class DraftChanged(val value: String) : TitleAction
    data class ReasoningSelected(val level: ReasoningLevelUi) : TitleAction
    data object AddAttachmentClicked : TitleAction
    data object SendClicked : TitleAction
    data object NewChatClicked : TitleAction
    data object OpenHistoryClicked : TitleAction
    data object OpenSettingsClicked : TitleAction
    data object OpenAboutClicked : TitleAction
}
