package platform.connectivity

import kotlinx.coroutines.flow.StateFlow

/** App-scoped OS network status (not a proof that Gemini is reachable). */
interface ConnectivityObserver {
    val isOnline: StateFlow<Boolean>

    /** Rechecks Android's currently validated default network, without an HTTP probe. */
    fun refresh(): Boolean
}
