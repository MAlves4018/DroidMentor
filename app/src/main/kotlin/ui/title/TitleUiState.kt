package ui.title

import ui.chat.model.ReasoningLevelUi

/** Immutable Title state. Capability availability is injected by future provider work. */
data class TitleUiState(
    val draft: String = "",
    val reasoningLevel: ReasoningLevelUi = ReasoningLevelUi.AUTO,
    val supportedReasoningLevels: Set<ReasoningLevelUi> = emptySet(),
)
