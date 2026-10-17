package platform.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import domain.model.identity.ConversationId

/**
 * Owns one ViewModelStore per conversation inside the Activity ViewModelStore.
 * A chat keeps its draft while another destination is visible, and deleting a chat
 * explicitly clears only that chat's ViewModels. Rotation retains this registry.
 */
class ChatViewModelScopes : ViewModel() {
    private class ChatOwner(override val viewModelStore: ViewModelStore) : ViewModelStoreOwner

    private val owners = mutableMapOf<ConversationId, ChatOwner>()

    fun ownerFor(id: ConversationId): ViewModelStoreOwner =
        owners.getOrPut(id) { ChatOwner(ViewModelStore()) }

    fun release(id: ConversationId) {
        owners.remove(id)?.viewModelStore?.clear()
    }

    internal fun isTracked(id: ConversationId): Boolean = id in owners

    override fun onCleared() {
        owners.values.forEach { it.viewModelStore.clear() }
        owners.clear()
    }
}
