package ui.settings

import data.InMemorySettingsRepository
import domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import test.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun initialStateReadsRepository() = runTest {
        val vm = SettingsViewModel(InMemorySettingsRepository())
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.apiKeyConfigured)
        assertEquals(SettingsPage.OVERVIEW, vm.uiState.value.page)
    }

    @Test fun navigationAndThemeRemainTyped() = runTest {
        val vm = SettingsViewModel(InMemorySettingsRepository())
        advanceUntilIdle()
        assertEquals(SettingsEffect.ToggleTheme, vm.onAction(SettingsAction.ToggleThemeClicked))
        assertEquals(SettingsEffect.OpenAbout, vm.onAction(SettingsAction.OpenAboutClicked))
        vm.onAction(SettingsAction.OpenApiKeyClicked)
        assertEquals(SettingsPage.API_KEY, vm.uiState.value.page)
        vm.onAction(SettingsAction.CloseApiKeyClicked)
        assertEquals(SettingsPage.OVERVIEW, vm.uiState.value.page)
    }

    @Test fun blankDraftIsRejectedWithoutPersisting() = runTest {
        val repository = InMemorySettingsRepository()
        val vm = SettingsViewModel(repository)
        advanceUntilIdle()
        vm.onAction(SettingsAction.ApiKeyChanged("   "))
        assertNull(vm.onAction(SettingsAction.SaveKeyClicked))
        advanceUntilIdle()
        assertNull(repository.getApiKey())
        assertFalse(vm.uiState.value.canSave)
    }

    @Test fun saveUpdatesConfiguredStateAndClearsSensitiveDraft() = runTest {
        val repository = InMemorySettingsRepository()
        val vm = SettingsViewModel(repository)
        advanceUntilIdle()
        vm.onAction(SettingsAction.ApiKeyChanged("  fake-local-key  "))
        vm.onAction(SettingsAction.ToggleApiKeyVisibility)
        assertNull(vm.onAction(SettingsAction.SaveKeyClicked))
        advanceUntilIdle()
        assertEquals("fake-local-key", repository.getApiKey())
        assertTrue(vm.uiState.value.apiKeyConfigured)
        assertEquals("", vm.uiState.value.apiKeyDraft)
        assertFalse(vm.uiState.value.apiKeyRevealed)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test fun removeRequiresConfirmThenClearsRepositoryAndDraft() = runTest {
        val repository = InMemorySettingsRepository()
        repository.saveApiKey("fake-local-key")
        val vm = SettingsViewModel(repository)
        advanceUntilIdle()
        vm.onAction(SettingsAction.RemoveKeyClicked)
        assertEquals("fake-local-key", repository.getApiKey())
        vm.onAction(SettingsAction.RequestRemoveKeyClicked)
        vm.onAction(SettingsAction.CancelRemoveKeyClicked)
        assertEquals("fake-local-key", repository.getApiKey())
        vm.onAction(SettingsAction.RequestRemoveKeyClicked)
        vm.onAction(SettingsAction.ApiKeyChanged("unsaved-draft"))
        vm.onAction(SettingsAction.RemoveKeyClicked)
        advanceUntilIdle()
        assertNull(repository.getApiKey())
        assertFalse(vm.uiState.value.apiKeyConfigured)
        assertFalse(vm.uiState.value.confirmRemoveKey)
        assertEquals("", vm.uiState.value.apiKeyDraft)
    }

    @Test fun failedSavePreservesDraftAndDoesNotExposeKeyInError() = runTest {
        val fake = object : SettingsRepository {
            override val hasApiKey: Flow<Boolean> = flowOf(false)
            override suspend fun getApiKey(): String? = null
            override suspend fun saveApiKey(value: String): Unit = error("fake-local-key should not appear")
            override suspend fun removeApiKey(): Unit = Unit
        }
        val vm = SettingsViewModel(fake)
        advanceUntilIdle()
        vm.onAction(SettingsAction.ApiKeyChanged("fake-local-key"))
        vm.onAction(SettingsAction.SaveKeyClicked)
        advanceUntilIdle()
        assertEquals("fake-local-key", vm.uiState.value.apiKeyDraft)
        assertFalse(vm.uiState.value.isSaving)
        assertFalse(vm.uiState.value.errorMessage.orEmpty().contains("fake-local-key"))
    }
}
