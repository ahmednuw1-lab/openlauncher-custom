package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reading the next turn from Google Maps' notification (1.4). */
class NavParseTest {

    private val maps = NavParse.GOOGLE_MAPS

    @Test fun distancesInEnglishAndArabic() {
        for (t in listOf("300 m", "1.2 km", "0,5 km", "500 ft", "2 mi", "300 م", "٣٠٠ م", "1.5 كم", "200 متر")) {
            assertTrue(t, NavParse.looksLikeDistance(t))
        }
        for (t in listOf("Turn right", "5 min", "Google Maps", "ETA 8:45", "انعطف يميناً", "")) {
            assertFalse(t, NavParse.looksLikeDistance(t))
        }
    }

    @Test fun onlyAnOngoingGoogleMapsNavigationNotification() {
        assertTrue(NavParse.isNav(maps, true, "navigation", null, "Turn right"))
        assertTrue(NavParse.isNav(maps, true, null, "navigation_notification_channel", ""))
        assertTrue(NavParse.isNav(maps, true, null, null, "300 m"))
        // Not ongoing, another app, or a Google Maps notification about something else
        assertFalse(NavParse.isNav(maps, false, "navigation", null, "300 m"))
        assertFalse(NavParse.isNav("com.whatsapp", true, "navigation", null, "300 m"))
        assertFalse(NavParse.isNav(maps, true, "status", "location_sharing", "Sharing your location"))
        assertFalse(NavParse.isNav(null, true, "navigation", null, "300 m"))
    }

    @Test fun textsAreTidiedAndKeptShort() {
        assertEquals("Turn right onto King Fahd Rd", NavParse.clean("  Turn right\nonto   King Fahd Rd "))
        assertEquals(10, NavParse.clean("x".repeat(50), 10).length)
        assertEquals("", NavParse.clean(null))
    }

    @Test fun theLongFormOfTheInstructionWins() {
        val i = NavParse.info("300 m", "King Fahd Rd", "8:45 ETA", "Turn right onto King Fahd Rd", null, maps, "k")!!
        assertEquals("300 m", i.title)
        assertEquals("Turn right onto King Fahd Rd", i.text)
        assertEquals("8:45 ETA", i.sub)
        assertEquals("k", i.key)
    }

    @Test fun anEmptyNotificationIsNoTurn() {
        assertNull(NavParse.info(" ", null, "8:45", null, null, maps, "k"))
        assertEquals("Head north", NavParse.info(null, "Head north", null, null, null, maps, "k")!!.text)
    }
}
