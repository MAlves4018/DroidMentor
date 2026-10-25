package ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import domain.repository.SettingsRepository

class SettingsViewModelFactory(
    private val repository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
        return SettingsViewModel(repository) as T
    }
}
