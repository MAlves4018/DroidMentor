package ui.title

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ui.navigation.NavigationAction


/** Owns Title presentation state; Android and Compose are not used by its actions. */
class TitleViewModel(initialState: TitleUiState = TitleUiState()) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<TitleUiState> = mutableUiState.asStateFlow()

    fun onAction(action: TitleAction): TitleEffect? = when (action) {
        is TitleAction.DraftChanged -> {
            mutableUiState.value = mutableUiState.value.copy(draft = action.value)
            null
        }

        is TitleAction.ReasoningSelected -> {
            if (action.level in mutableUiState.value.supportedReasoningLevels) {
                mutableUiState.value = mutableUiState.value.copy(reasoningLevel = action.level)
            }
            null
        }
        TitleAction.AddAttachmentClicked -> null

        TitleAction.NewChatClicked -> TitleEffect.Navigate(NavigationAction.NewChat)
        TitleAction.OpenHistoryClicked -> TitleEffect.Navigate(NavigationAction.OpenHistory)
        TitleAction.OpenSettingsClicked -> TitleEffect.Navigate(NavigationAction.OpenSettings)
        TitleAction.OpenAboutClicked -> TitleEffect.Navigate(NavigationAction.OpenAbout)

        // A prompt handoff contract is not part of ARCH-002. Do not pretend the
        // typed draft was sent, or create a conversation while losing its text.
        TitleAction.SendClicked -> null
    }
}
