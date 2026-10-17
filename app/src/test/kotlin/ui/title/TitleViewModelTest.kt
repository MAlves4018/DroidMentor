package ui.title

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.navigation.NavigationAction

class TitleViewModelTest {
    @Test fun initialState_isEmpty() {
        val model = TitleViewModel()
        assertEquals(TitleUiState(), model.uiState.value)
    }

    @Test fun draftChange_updatesImmutableSnapshot() {
        val model = TitleViewModel()
        val initial = model.uiState.value
        assertNull(model.onAction(TitleAction.DraftChanged("hello")))
        assertEquals("hello", model.uiState.value.draft)
        assertEquals("", initial.draft)
    }

    @Test fun multilineDraft_isNotSilentlyTrimmed() {
        val model = TitleViewModel()
        model.onAction(TitleAction.DraftChanged(" hi\nworld "))
        assertEquals(" hi\nworld ", model.uiState.value.draft)
    }

    @Test fun newChat_emitsNavigationWithoutMutatingDraft() {
        val model = TitleViewModel()
        model.onAction(TitleAction.DraftChanged("draft"))
        assertEquals(TitleEffect.Navigate(NavigationAction.NewChat), model.onAction(TitleAction.NewChatClicked))
        assertEquals("draft", model.uiState.value.draft)
    }

    @Test fun history_emitsNavigation() {
        assertEquals(TitleEffect.Navigate(NavigationAction.OpenHistory), TitleViewModel().onAction(TitleAction.OpenHistoryClicked))
    }

    @Test fun settings_emitsNavigation() {
        assertEquals(TitleEffect.Navigate(NavigationAction.OpenSettings), TitleViewModel().onAction(TitleAction.OpenSettingsClicked))
    }

    @Test fun about_emitsNavigation() {
        assertEquals(TitleEffect.Navigate(NavigationAction.OpenAbout), TitleViewModel().onAction(TitleAction.OpenAboutClicked))
    }

    @Test fun send_doesNotFakeSubmittingOrLoseDraft() {
        val model = TitleViewModel()
        model.onAction(TitleAction.DraftChanged("not yet connected"))
        assertNull(model.onAction(TitleAction.SendClicked))
        assertEquals("not yet connected", model.uiState.value.draft)
    }

    @Test fun repeatedCommands_dontAccumulateOneOffEventsInState() {
        val model = TitleViewModel()
        repeat(3) {
            assertEquals(TitleEffect.Navigate(NavigationAction.OpenSettings), model.onAction(TitleAction.OpenSettingsClicked))
        }
        assertEquals(TitleUiState(), model.uiState.value)
    }

    @Test fun stateIsIndependentAcrossViewModelInstances() {
        val a = TitleViewModel()
        val b = TitleViewModel()
        a.onAction(TitleAction.DraftChanged("A"))
        assertEquals("A", a.uiState.value.draft)
        assertEquals("", b.uiState.value.draft)
    }

    @Test fun initializedDraftIsPreserved() {
        val model = TitleViewModel(TitleUiState("existing"))
        assertTrue(model.uiState.value.draft.isNotBlank())
        assertEquals("existing", model.uiState.value.draft)
    }
}
