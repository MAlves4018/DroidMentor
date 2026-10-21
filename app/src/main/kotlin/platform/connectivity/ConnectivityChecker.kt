package platform.connectivity


/**
 * Checks whether the device currently has network connectivity
 * before attempting to communicate with Gemini.
 * Network availability does not guarantee that Gemini
 * is reachable.
 *
 * This interface allows different implementations:
 * - AndroidConnectivityChecker for real devices.
 * - FakeConnectivityChecker for deterministic tests.
 *
 */
fun interface ConnectivityChecker {

    fun isConnected(): Boolean
}
