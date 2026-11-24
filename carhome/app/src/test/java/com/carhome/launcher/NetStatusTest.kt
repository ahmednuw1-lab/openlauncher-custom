package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The internet sign (1.3.3): CarHome's own check decides, not Android's "validated" flag. */
class NetStatusTest {

    private fun of(link: NetLink, probe: Probe, wifiOn: Boolean = true, portal: Boolean = false, slow: Boolean = false) =
        NetStatus.of(link, wifiOn, portal, probe, slow)

    @Test fun noNetworkIsWifiOffOrNotConnected() {
        assertEquals(NetState.WIFI_OFF, of(NetLink.NONE, Probe.NONE, wifiOn = false))
        assertEquals(NetState.NOT_CONNECTED, of(NetLink.NONE, Probe.NONE, wifiOn = true))
        // An old answer never shows once the network is gone
        assertEquals(NetState.NOT_CONNECTED, of(NetLink.NONE, Probe.OK))
        assertEquals(NetState.WIFI_OFF, of(NetLink.NONE, Probe.OK, wifiOn = false))
    }

    @Test fun workingInternetShowsHowTheBoxIsConnected() {
        assertEquals(NetState.CONNECTED, of(NetLink.WIFI, Probe.OK))
        assertEquals(NetState.CONNECTED, of(NetLink.CABLE, Probe.OK))
        assertEquals(NetState.CONNECTED, of(NetLink.OTHER, Probe.OK))
        assertEquals(NetState.HOTSPOT, of(NetLink.HOTSPOT, Probe.OK))
        assertEquals(NetState.CELLULAR, of(NetLink.CELLULAR, Probe.OK))
        // Mobile data with Wi-Fi switched off is still mobile data
        assertEquals(NetState.CELLULAR, of(NetLink.CELLULAR, Probe.OK, wifiOn = false))
    }

    @Test fun theHotspotWithNoDataIsNoInternet() {
        // The bug in 1.3.2: Android said "validated" (its check switched off on the box), so the sign stayed green
        assertEquals(NetState.NO_INTERNET, of(NetLink.HOTSPOT, Probe.FAILED))
        assertEquals(NetState.NO_INTERNET, of(NetLink.WIFI, Probe.FAILED))
        assertEquals(NetState.NO_INTERNET, of(NetLink.CELLULAR, Probe.FAILED))
    }

    @Test fun aCableWithNoInternetIsNotANetworkYouJoined() {
        assertEquals(NetState.NOT_CONNECTED, of(NetLink.CABLE, Probe.FAILED))
        assertEquals(NetState.WIFI_OFF, of(NetLink.OTHER, Probe.FAILED, wifiOn = false))
    }

    @Test fun checkingUntilTheFirstAnswer() {
        assertEquals(NetState.CHECKING, of(NetLink.WIFI, Probe.NONE))
        assertEquals(NetState.CHECKING, of(NetLink.CELLULAR, Probe.NONE))
    }

    @Test fun signInPage() {
        assertEquals(NetState.SIGN_IN, of(NetLink.WIFI, Probe.PORTAL))
        // Android found the sign-in page before our first answer, or our check failed
        assertEquals(NetState.SIGN_IN, of(NetLink.WIFI, Probe.NONE, portal = true))
        assertEquals(NetState.SIGN_IN, of(NetLink.WIFI, Probe.FAILED, portal = true))
        // Signed in: our answer works even if Android hasn't looked again yet
        assertEquals(NetState.CONNECTED, of(NetLink.WIFI, Probe.OK, portal = true))
    }

    @Test fun slowBeatsTheKindOfLink() {
        assertEquals(NetState.SLOW, of(NetLink.WIFI, Probe.OK, slow = true))
        assertEquals(NetState.SLOW, of(NetLink.CELLULAR, Probe.OK, slow = true))
        // Slow means nothing without an answer
        assertEquals(NetState.NO_INTERNET, of(NetLink.HOTSPOT, Probe.FAILED, slow = true))
    }

    @Test fun slowNeedsTwoSlowAnswersInARow() {
        assertFalse(NetStatus.slow(emptyList()))
        assertFalse(NetStatus.slow(listOf(3_000L)))
        assertFalse(NetStatus.slow(listOf(3_000L, 200L)))
        assertFalse(NetStatus.slow(listOf(200L, 3_000L)))
        assertTrue(NetStatus.slow(listOf(1_600L, 2_400L)))
        assertTrue(NetStatus.slow(listOf(100L, 1_600L, 2_400L)))
        assertFalse(NetStatus.slow(listOf(1_500L, 1_500L)))
    }

    @Test fun theAnswerToTheCheck() {
        assertEquals(Probe.OK, NetStatus.probeResult(204, 0))
        assertEquals(Probe.OK, NetStatus.probeResult(204, -1))
        assertEquals(Probe.OK, NetStatus.probeResult(200, 0))
        // A page or a redirect: a sign-in Wi-Fi or a carrier's top-up page
        assertEquals(Probe.PORTAL, NetStatus.probeResult(200, 512))
        assertEquals(Probe.PORTAL, NetStatus.probeResult(200, -1))
        assertEquals(Probe.PORTAL, NetStatus.probeResult(302, -1))
        assertEquals(Probe.PORTAL, NetStatus.probeResult(511, 100))
        assertEquals(Probe.FAILED, NetStatus.probeResult(404, 10))
        assertEquals(Probe.FAILED, NetStatus.probeResult(503, 10))
        assertEquals(Probe.FAILED, NetStatus.probeResult(-1, -1))
    }

    @Test fun theKindOfLink() {
        assertEquals(NetLink.NONE, NetStatus.linkOf(internet = false, cellular = false, wifi = true, notMetered = true, cable = false))
        assertEquals(NetLink.WIFI, NetStatus.linkOf(internet = true, cellular = false, wifi = true, notMetered = true, cable = false))
        // Android marks a phone's hotspot as metered
        assertEquals(NetLink.HOTSPOT, NetStatus.linkOf(internet = true, cellular = false, wifi = true, notMetered = false, cable = false))
        assertEquals(NetLink.CELLULAR, NetStatus.linkOf(internet = true, cellular = true, wifi = false, notMetered = false, cable = false))
        assertEquals(NetLink.CABLE, NetStatus.linkOf(internet = true, cellular = false, wifi = false, notMetered = true, cable = true))
        assertEquals(NetLink.OTHER, NetStatus.linkOf(internet = true, cellular = false, wifi = false, notMetered = true, cable = false))
    }

    @Test fun checkSoonerWhileThereIsAProblem() {
        assertEquals(NetStatus.CHECK_EVERY_MS, NetStatus.nextCheckMs(NetState.CONNECTED))
        assertEquals(NetStatus.CHECK_EVERY_MS, NetStatus.nextCheckMs(NetState.HOTSPOT))
        assertEquals(NetStatus.CHECK_EVERY_MS, NetStatus.nextCheckMs(NetState.CELLULAR))
        for (s in listOf(NetState.SLOW, NetState.NO_INTERNET, NetState.SIGN_IN, NetState.CHECKING))
            assertEquals(NetStatus.RECHECK_MS, NetStatus.nextCheckMs(s))
    }

    @Test fun onlineStates() {
        assertEquals(
            setOf(NetState.CONNECTED, NetState.HOTSPOT, NetState.CELLULAR, NetState.SLOW),
            NetState.entries.filter { it.online }.toSet()
        )
    }

    @Test fun theChecksArePlainHttpToTheTwoKnownHosts() {
        // These two hosts are the only plain-HTTP exceptions in res/xml/network_security_config.xml
        assertEquals(
            listOf("connectivitycheck.gstatic.com", "cp.cloudflare.com"),
            NetStatus.CHECK_URLS.map { java.net.URI(it).host }
        )
        assertTrue(NetStatus.CHECK_URLS.all { it.startsWith("http://") && it.endsWith("/generate_204") })
    }
}
