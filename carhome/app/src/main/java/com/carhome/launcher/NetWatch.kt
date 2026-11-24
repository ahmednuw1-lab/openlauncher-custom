package com.carhome.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

/** What the internet sign shows (1.3.3). The colours are in the UI. */
enum class NetState {
    /** Wi-Fi (or a cable) and the internet answers — green */
    CONNECTED,
    /** The phone's hotspot (a Wi-Fi Android marks as metered) and the internet answers — blue */
    HOTSPOT,
    /** The box's own mobile data (SIM) and the internet answers — blue */
    CELLULAR,
    /** The internet answers, but slowly — amber */
    SLOW,
    /** Just joined a network; checking that the internet answers — light blue, pulsing */
    CHECKING,
    /** A page wants something first: a café or hotel Wi-Fi sign-in, or a carrier's "top up" page — purple */
    SIGN_IN,
    /** On a network (e.g. the phone's hotspot with no data) but the internet doesn't answer — red */
    NO_INTERNET,
    /** Wi-Fi is on but hasn't joined a network — grey */
    NOT_CONNECTED,
    /** Wi-Fi is off and there's no other connection — grey */
    WIFI_OFF;

    /** The internet answers (green, blue or amber) */
    val online: Boolean get() = this == CONNECTED || this == HOTSPOT || this == CELLULAR || this == SLOW
}

/** How the box reaches the network Android uses for the internet. */
enum class NetLink { NONE, WIFI, HOTSPOT, CELLULAR, CABLE, OTHER }

/** One check of the internet: a tiny request that must come back empty (HTTP 204). */
enum class Probe { NONE, OK, PORTAL, FAILED }

/** The internet sign's rules, kept free of Android so they can be tested. */
object NetStatus {
    /**
     * The same empty answer Android itself asks for to see that the internet works
     * (plain HTTP on purpose: a sign-in Wi-Fi can only redirect plain HTTP). The second is a spare.
     */
    val CHECK_URLS = listOf(
        "http://connectivitycheck.gstatic.com/generate_204",
        "http://cp.cloudflare.com/generate_204"
    )
    const val TIMEOUT_MS = 4_000
    /** Answers slower than this, twice in a row, mark the internet as slow */
    const val SLOW_MS = 1_500L
    /** How often the internet is checked while the sign is on screen… */
    const val CHECK_EVERY_MS = 30_000L
    /** …and sooner while it is down, slow or wants a sign-in, to see it come back */
    const val RECHECK_MS = 10_000L

    fun linkOf(internet: Boolean, cellular: Boolean, wifi: Boolean, notMetered: Boolean, cable: Boolean): NetLink = when {
        !internet -> NetLink.NONE
        cellular -> NetLink.CELLULAR
        wifi -> if (notMetered) NetLink.WIFI else NetLink.HOTSPOT
        cable -> NetLink.CABLE
        else -> NetLink.OTHER
    }

    /**
     * Our own check decides, not Android's "validated" flag: many car boxes switch Android's
     * check off, and then every network looks validated even with no internet behind it.
     */
    fun of(link: NetLink, wifiOn: Boolean, portal: Boolean, probe: Probe, slow: Boolean): NetState = when {
        link == NetLink.NONE -> off(wifiOn)
        probe == Probe.OK -> when {
            slow -> NetState.SLOW
            link == NetLink.CELLULAR -> NetState.CELLULAR
            link == NetLink.HOTSPOT -> NetState.HOTSPOT
            else -> NetState.CONNECTED
        }
        probe == Probe.PORTAL || portal -> NetState.SIGN_IN
        probe == Probe.NONE -> NetState.CHECKING
        // A cable or other link with no internet (such as the box's own link to the car) isn't a network you joined
        link == NetLink.CABLE || link == NetLink.OTHER -> off(wifiOn)
        else -> NetState.NO_INTERNET
    }

    private fun off(wifiOn: Boolean) = if (wifiOn) NetState.NOT_CONNECTED else NetState.WIFI_OFF

    /** 204 (or an empty 200) is the internet; any other page or redirect is a sign-in or carrier page. */
    fun probeResult(code: Int, length: Long): Probe = when {
        code == 204 || (code == 200 && length == 0L) -> Probe.OK
        code in 200..399 || code == 511 -> Probe.PORTAL
        else -> Probe.FAILED
    }

    /** One slow answer can be a hiccup; the last two both slow is a slow connection. */
    fun slow(times: List<Long>): Boolean = times.size >= 2 && times.takeLast(2).all { it > SLOW_MS }

    fun nextCheckMs(state: NetState): Long = when (state) {
        NetState.CONNECTED, NetState.HOTSPOT, NetState.CELLULAR -> CHECK_EVERY_MS
        else -> RECHECK_MS
    }
}

/**
 * Follows the box's internet for the internet sign. Android says which network is in use;
 * whether the internet really answers is checked by CarHome itself, only while the sign is on screen.
 */
class NetWatch(context: Context) {
    private val app = context.applicationContext
    private val cm = app.getSystemService(ConnectivityManager::class.java)
    private val wifi = app.getSystemService(WifiManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Wakes the checking loop when the network changes */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    // Guarded by [lock]: the network in use and what is known about it
    private val lock = Any()
    private var network: Network? = null
    private var link = NetLink.NONE
    private var portal = false
    private var validated = false
    private var probe = Probe.NONE
    private val times = ArrayDeque<Long>()

    private val _state = MutableStateFlow(NetState.CHECKING)
    val state: StateFlow<NetState> = _state.asStateFlow()

    /** Unknown counts as on, so a box that won't say shows "not connected" rather than "Wi-Fi off". */
    private fun wifiOn(): Boolean = runCatching { wifi?.isWifiEnabled ?: true }.getOrDefault(true)

    private fun linkOf(caps: NetworkCapabilities?): NetLink {
        if (caps == null) return NetLink.NONE
        return NetStatus.linkOf(
            internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            notMetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            cable = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                (Build.VERSION.SDK_INT >= 31 && caps.hasTransport(NetworkCapabilities.TRANSPORT_USB))
        )
    }

    /** Takes what Android says about the network in use. A new network, or a new kind of link, is checked again. */
    private fun take(net: Network?, caps: NetworkCapabilities?) {
        val newLink = if (net == null) NetLink.NONE else linkOf(caps)
        val newPortal = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true
        val newValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val changed = synchronized(lock) {
            val moved = net != network || newLink != link
            val changed = moved || newPortal != portal || newValidated != validated
            if (moved) { probe = Probe.NONE; times.clear() }
            network = net; link = newLink; portal = newPortal; validated = newValidated
            changed
        }
        // Signal strength changes come here too, often; only real changes count
        if (changed) { publish(); wake.trySend(Unit) }
    }

    private fun publish(): NetState {
        val on = wifiOn()
        val s = synchronized(lock) { NetStatus.of(link, on, portal, probe, NetStatus.slow(times.toList())) }
        _state.value = s
        return s
    }

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = take(network, caps)
        // For the default network this comes only when no other network takes over: nothing is connected.
        // (Asking Android for the active network here could still return the one just lost.)
        override fun onLost(network: Network) {
            if (synchronized(lock) { this@NetWatch.network == network }) take(null, null)
        }
        override fun onUnavailable() = take(null, null)
    }

    /** Asks for the empty answer through [net]; a slower answer than usual is remembered for "slow". */
    private fun check(net: Network): Pair<Probe, Long> {
        for (url in NetStatus.CHECK_URLS) {
            val start = SystemClock.elapsedRealtime()
            val r = runCatching {
                val c = net.openConnection(URL(url)) as HttpURLConnection
                try {
                    c.connectTimeout = NetStatus.TIMEOUT_MS
                    c.readTimeout = NetStatus.TIMEOUT_MS
                    c.instanceFollowRedirects = false
                    c.useCaches = false
                    NetStatus.probeResult(c.responseCode, c.contentLengthLong)
                } finally {
                    c.disconnect()
                }
            }.getOrDefault(Probe.FAILED)
            if (r != Probe.FAILED) return r to (SystemClock.elapsedRealtime() - start)
        }
        return Probe.FAILED to 0L
    }

    /** Runs while the sign is on screen: checks now, then every 30 s (10 s while there's a problem). */
    private suspend fun watch() {
        while (true) {
            val net = synchronized(lock) { network }
            if (net == null) {
                publish()
                // With no network only Wi-Fi being switched on or off changes the sign; look again now and then
                withTimeoutOrNull(3_000) { wake.receive() }
                continue
            }
            val (result, ms) = check(net)
            synchronized(lock) {
                if (network == net) {
                    probe = result
                    if (result == Probe.OK) {
                        times.addLast(ms)
                        while (times.size > 3) times.removeFirst()
                    } else times.clear()
                }
            }
            val s = publish()
            withTimeoutOrNull(NetStatus.nextCheckMs(s)) { wake.receive() }
        }
    }

    init {
        runCatching {
            val n = cm?.activeNetwork
            take(n, n?.let { cm?.getNetworkCapabilities(it) })
        }
        publish()
        // Needs only normal permissions (network state, Wi-Fi state); without them the sign shows "not connected"
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
        // Check the internet only while the sign is on screen, i.e. while someone follows [state]
        scope.launch {
            _state.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { watched ->
                if (watched) watch()
            }
        }
    }
}
