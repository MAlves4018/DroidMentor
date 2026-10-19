package ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test fun initialState_showsOverview() {
        val vm = SettingsViewModel()
        assertEquals(SettingsPage.OVERVIEW, vm.uiState.value.page)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test fun themeSwitch_emitsCommandButDoesNotOwnGlobalTheme() {
        val vm = SettingsViewModel()
        assertEquals(SettingsEffect.ToggleTheme, vm.onAction(SettingsAction.ToggleThemeClicked))
        assertEquals(SettingsPage.OVERVIEW, vm.uiState.value.page)
    }

    @Test fun apiKeySubpage_canOpenCloseWithoutLosingDraft() {
        val vm = SettingsViewModel()
        assertNull(vm.onAction(SettingsAction.OpenApiKeyClicked))
        assertEquals(SettingsPage.API_KEY, vm.uiState.value.page)
        vm.onAction(SettingsAction.ApiKeyChanged("not-real"))
        vm.onAction(SettingsAction.ToggleApiKeyVisibility)
        assertTrue(vm.uiState.value.apiKeyRevealed)
        vm.onAction(SettingsAction.CloseApiKeyClicked)
        assertEquals(SettingsPage.OVERVIEW, vm.uiState.value.page)
        assertEquals("not-real", vm.uiState.value.apiKeyDraft)
        assertFalse(vm.uiState.value.apiKeyRevealed)
    }

    @Test fun saveWithoutDraft_isIgnored() {
        val vm = SettingsViewModel()
        assertNull(vm.onAction(SettingsAction.SaveKeyClicked))
        vm.onAction(SettingsAction.ApiKeyChanged("  "))
        assertNull(vm.onAction(SettingsAction.SaveKeyClicked))
    }

    @Test fun saveWithDraft_emitsCommandWithoutPretendingToPersist() {
        val vm = SettingsViewModel()
        vm.onAction(SettingsAction.ApiKeyChanged("dummy"))
        assertEquals(SettingsEffect.SaveKey("dummy"), vm.onAction(SettingsAction.SaveKeyClicked))
        assertFalse(vm.uiState.value.apiKeyConfigured)
    }

    @Test fun removeRequiresConfirmationAndConfiguredState() {
        val empty = SettingsViewModel()
        empty.onAction(SettingsAction.RequestRemoveKeyClicked)
        assertFalse(empty.uiState.value.confirmRemoveKey)
        assertNull(empty.onAction(SettingsAction.RemoveKeyClicked))
        val vm = SettingsViewModel(SettingsUiState(apiKeyConfigured = true))
        assertNull(vm.onAction(SettingsAction.RemoveKeyClicked))
        vm.onAction(SettingsAction.RequestRemoveKeyClicked)
        assertTrue(vm.uiState.value.confirmRemoveKey)
        vm.onAction(SettingsAction.CancelRemoveKeyClicked)
        assertFalse(vm.uiState.value.confirmRemoveKey)
        vm.onAction(SettingsAction.RequestRemoveKeyClicked)
        assertEquals(SettingsEffect.RemoveKey, vm.onAction(SettingsAction.RemoveKeyClicked))
        assertFalse(vm.uiState.value.confirmRemoveKey)
    }

    @Test fun aboutRemainsGlobalNavigationEffect() {
        assertEquals(SettingsEffect.OpenAbout, SettingsViewModel().onAction(SettingsAction.OpenAboutClicked))
    }

    @Test
    fun updateApiKeyConfigured_trueUpdatesState() {
        val vm = SettingsViewModel()

        assertFalse(vm.uiState.value.apiKeyConfigured)

        vm.updateApiKeyConfigured(true)

        assertTrue(vm.uiState.value.apiKeyConfigured)
    }

    @Test
    fun updateApiKeyConfigured_falseUpdatesState() {
        val vm = SettingsViewModel(
            SettingsUiState(apiKeyConfigured = true)
        )

        assertTrue(vm.uiState.value.apiKeyConfigured)

        vm.updateApiKeyConfigured(false)

        assertFalse(vm.uiState.value.apiKeyConfigured)
    }

    @Test
    fun updateApiKeyConfigured_preservesOtherUiState() {
        val vm = SettingsViewModel()

        vm.onAction(SettingsAction.OpenApiKeyClicked)
        vm.onAction(SettingsAction.ApiKeyChanged("draft-key"))

        vm.updateApiKeyConfigured(true)

        assertTrue(vm.uiState.value.apiKeyConfigured)
        assertEquals(
            SettingsPage.API_KEY,
            vm.uiState.value.page
        )
        assertEquals(
            "draft-key",
            vm.uiState.value.apiKeyDraft
        )
    }

    @Test
    fun repositoryStatus_allowsRemoveConfirmation() {
        val vm = SettingsViewModel()

        vm.updateApiKeyConfigured(true)

        vm.onAction(SettingsAction.RequestRemoveKeyClicked)

        assertTrue(vm.uiState.value.confirmRemoveKey)

        assertEquals(
            SettingsEffect.RemoveKey,
            vm.onAction(SettingsAction.RemoveKeyClicked)
        )
    }
}
