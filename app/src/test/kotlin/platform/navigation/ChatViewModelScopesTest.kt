package platform.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelStore
import domain.model.identity.ConversationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatViewModelScopesTest {
    private class TrackingModel : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    private fun model(owner: ViewModelStoreOwner): TrackingModel =
        ViewModelProvider(owner, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackingModel() as T
        })[TrackingModel::class.java]

    @Test fun sameChatRetainsOwnerAndModelAcrossNavigation() {
        val scopes = ChatViewModelScopes()
        val id = ConversationId("A")
        val owner = scopes.ownerFor(id)
        val vm = model(owner)
        assertSame(owner, scopes.ownerFor(id))
        assertSame(vm, model(scopes.ownerFor(id)))
        assertFalse(vm.cleared)
    }

    @Test fun twoChatsHaveIndependentStores() {
        val scopes = ChatViewModelScopes()
        val a = scopes.ownerFor(ConversationId("A"))
        val b = scopes.ownerFor(ConversationId("B"))
        assertNotSame(a, b)
        assertNotSame(model(a), model(b))
    }

    @Test fun deletingChatClearsOnlyItsModel() {
        val scopes = ChatViewModelScopes()
        val aId = ConversationId("A")
        val bId = ConversationId("B")
        val a = model(scopes.ownerFor(aId))
        val b = model(scopes.ownerFor(bId))
        scopes.release(aId)
        assertTrue(a.cleared)
        assertFalse(b.cleared)
        assertFalse(scopes.isTracked(aId))
        assertTrue(scopes.isTracked(bId))
        assertSame(b, model(scopes.ownerFor(bId)))
    }

    @Test fun releasingUnknownOrSameIdTwiceIsSafe() {
        val scopes = ChatViewModelScopes()
        val id = ConversationId("missing")
        scopes.release(id)
        scopes.ownerFor(id)
        scopes.release(id)
        scopes.release(id)
        assertFalse(scopes.isTracked(id))
    }

    @Test fun activityOwnerCanRetainRegistryAcrossConfigurationChange() {
        val activityStore = ViewModelStore()
        val activityOwner = object : ViewModelStoreOwner {
            override val viewModelStore = activityStore
        }
        val registryFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModelScopes() as T
        }
        val first = ViewModelProvider(activityOwner, registryFactory)[ChatViewModelScopes::class.java]
        val conversationOwner = first.ownerFor(ConversationId("A"))
        val draftOwner = model(conversationOwner)
        val second = ViewModelProvider(activityOwner, registryFactory)[ChatViewModelScopes::class.java]
        assertSame(first, second)
        assertSame(draftOwner, model(second.ownerFor(ConversationId("A"))))
        activityStore.clear()
        assertTrue(draftOwner.cleared)
    }

    @Test fun destroyingActivityClearsEveryConversationStore() {
        val scopes = ChatViewModelScopes()
        val a = model(scopes.ownerFor(ConversationId("A")))
        val b = model(scopes.ownerFor(ConversationId("B")))
        // Simulate the parent store ownership in a testable way.
        val root = ViewModelStore()
        val rootOwner = object : ViewModelStoreOwner {
            override val viewModelStore = root
        }
        // A separate registry registered in the parent's store owns scoped models.
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModelScopes() as T
        }
        val managed = ViewModelProvider(rootOwner, factory)[ChatViewModelScopes::class.java]
        val c = model(managed.ownerFor(ConversationId("C")))
        val d = model(managed.ownerFor(ConversationId("D")))
        root.clear()
        assertTrue(c.cleared)
        assertTrue(d.cleared)
        // Unrelated registries are independent.
        assertFalse(a.cleared)
        assertFalse(b.cleared)
    }

    @Test fun recreatedIdGetsNewOwnerAndViewModel() {
        val scopes = ChatViewModelScopes()
        val id = ConversationId("A")
        val previousOwner = scopes.ownerFor(id)
        val previous = model(previousOwner)
        scopes.release(id)
        val nextOwner = scopes.ownerFor(id)
        val next = model(nextOwner)
        assertNotSame(previousOwner, nextOwner)
        assertNotSame(previous, next)
        assertTrue(previous.cleared)
        assertFalse(next.cleared)
    }
}
