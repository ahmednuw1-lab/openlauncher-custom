package com.carhome.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ONLINE = the internet works; NO_INTERNET = on a network (e.g. the phone's hotspot) that has no internet; OFFLINE = no network. */
enum class NetState { ONLINE, NO_INTERNET, OFFLINE }

object NetStatus {
    /** [connected]: a network that offers internet; [validated]: Android checked that it really reaches the internet. */
    fun of(connected: Boolean, validated: Boolean): NetState = when {
        !connected -> NetState.OFFLINE
        validated -> NetState.ONLINE
        else -> NetState.NO_INTERNET
    }
}

/** Follows the box's internet connection for the internet sign on the home screen. */
class NetWatch(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val _state = MutableStateFlow(current())
    val state: StateFlow<NetState> = _state.asStateFlow()

    private fun stateOf(caps: NetworkCapabilities?): NetState = NetStatus.of(
        connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
        validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    )

    private fun current(): NetState = runCatching {
        val c = cm ?: return NetState.OFFLINE
        stateOf(c.getNetworkCapabilities(c.activeNetwork))
    }.getOrDefault(NetState.OFFLINE)

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { _state.value = stateOf(caps) }
        override fun onLost(network: Network) { _state.value = current() }
        override fun onUnavailable() { _state.value = NetState.OFFLINE }
    }

    init {
        // Needs only the normal ACCESS_NETWORK_STATE permission; a box without it just shows "offline"
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
    }
}
