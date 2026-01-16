package com.truckershub.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * NETWORK MANAGER
 *
 * Überwacht und prüft netzwerk-verbindung
 * Echtzeit-Updates der Verbindung
 */
class NetworkMonitor(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.postValue(true)
        }

        override fun onLost(network: Network) {
            _isConnected.postValue(false)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val isConnected =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            _isConnected.postValue(isConnected)
        }
    }

    /**
     * Startet Netzwerk-Monitoring
     */
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Initiale Prüfung
        _isConnected.value = isCurrentlyConnected()
    }

    /**
     * Stoppt Netzwerk-Monitoring
     */
    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    /**
     * Momentane Verbindung prüfen (einmalig)
     */
    fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/**
 * ONLINE STATUS COMPOSABLE
 *
 * Prüft und zeigt Online-Status in Compose
 */
@Composable
fun rememberOnlineStatus(context: Context): State<Boolean> {
    var isOnline by remember { mutableStateOf(true) }
    val networkMonitor = remember { NetworkMonitor(context) }

    // Initial check
    androidx.compose.runtime.LaunchedEffect(Unit) {
        networkMonitor.startMonitoring()
    }

    // Cleanup beim Verlassen
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            networkMonitor.stopMonitoring()
        }
    }

    // Einfache alternative Falls collectAsStateWithLifecycle nicht verfügbar
    return androidx.compose.runtime.produceState(initialValue = true) {
        networkMonitor.isConnected.asFlow().collect { value ->
            this.value = value
        }
    }
}

/**
 * EINFACHE ONLINE PRÜFUNG (ohne Live-Updates)
 *
 * Für einmalige Prüfung
 */
fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}