package platform.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Legacy in-memory test constructor fallback; production injects AndroidConnectivityObserver. */
object AlwaysOnlineConnectivityObserver : ConnectivityObserver {
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true)
    override fun refresh(): Boolean = true
}
