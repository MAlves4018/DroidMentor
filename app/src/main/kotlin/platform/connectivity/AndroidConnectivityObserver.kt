package platform.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One observer per Application. Listening to Android network callbacks avoids polling.
 * Never invoke ConnectivityManager's synchronous network queries from a callback:
 * the capabilities supplied by onCapabilitiesChanged are authoritative for that event.
 */
class AndroidConnectivityObserver(
    context: Context,
    private val checker: ConnectivityChecker,
) : ConnectivityObserver {
    private val manager = context.applicationContext.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager

    private val state = MutableStateFlow(checker.isConnected())
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()

    @Volatile
    private var currentNetwork: Network? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            currentNetwork = network
            // Availability alone does not establish validated internet access.
            // Wait for onCapabilitiesChanged before publishing a positive state.
            state.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            if (currentNetwork == null) currentNetwork = network
            if (currentNetwork != network) return
            state.value = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) && networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
        }

        override fun onLost(network: Network) {
            if (currentNetwork == network) {
                currentNetwork = null
                state.value = false
            }
        }
    }

    init {
        manager.registerDefaultNetworkCallback(callback)
    }

    override fun refresh(): Boolean = checker.isConnected().also { state.value = it }

    /** Optional for tests; production owns this singleton for the Application lifetime. */
    fun close() = manager.unregisterNetworkCallback(callback)
}
