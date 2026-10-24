package platform.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Android implementation of ConnectivityChecker.
 *
 * Uses ConnectivityManager to verify that the device
 * has an active network with validated internet access.
 */
class AndroidConnectivityChecker(
    context: Context
) : ConnectivityChecker {

    private val connectivityManager =
        context.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

    /**
     * Returns true when the active network:
     * - Has internet capability.
     * - Has been validated by Android.
     *
     * Returns false if no active network is available.
     */
    override fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork
            ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }
}
