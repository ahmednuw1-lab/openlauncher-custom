package com.carhome.launcher

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** 1.3: the Qibla, the time in words, the internet sign and the new home-style settings. */
class HomeStyleTest {

    private fun near(expected: Double, actual: Double, within: Double = 0.5) =
        assertEquals(expected, actual, within)

    @Test fun qiblaFromSaudiCitiesAndAbroad() {
        near(248.1, Qibla.bearing(25.3832, 49.5872))   // Al-Hofuf
        near(243.8, Qibla.bearing(24.7136, 46.6753))   // Riyadh
        near(101.1, Qibla.bearing(21.5433, 39.1728))   // Jeddah
        near(119.0, Qibla.bearing(51.5074, -0.1278))   // London
        near(295.2, Qibla.bearing(-6.2088, 106.8456))  // Jakarta
    }

    @Test fun qiblaAtTheKaabaIsDefined() {
        assertEquals(0.0, Qibla.bearing(Qibla.KAABA_LAT, Qibla.KAABA_LON), 0.0)
    }

    @Test fun qiblaRelativeToTheCar() {
        near(0.0, Qibla.relative(248.0, 248.0))
        near(90.0, Qibla.relative(248.0, 158.0))     // to the right
        near(270.0, Qibla.relative(10.0, 100.0))     // to the left, across north
    }

    @Test fun distanceBetweenHofufAndRiyadh() {
        val km = Geo.distanceM(25.3832, 49.5872, 24.7136, 46.6753) / 1000.0
        near(301.0, km, 6.0)
    }

    @Test fun timeInWordsEnglish() {
        assertEquals("twenty to one", TimeWords.phrase(12, 39, false))
        assertEquals("twelve o'clock", TimeWords.phrase(0, 0, false))
        assertEquals("twelve o'clock", TimeWords.phrase(23, 58, false))   // rounds up past midnight
        assertEquals("quarter past nine", TimeWords.phrase(21, 14, false))
        assertEquals("half past three", TimeWords.phrase(15, 30, false))
        assertEquals("five to twelve", TimeWords.phrase(11, 55, false))
    }

    @Test fun timeInWordsArabic() {
        assertEquals("الساعة الواحدة إلا ثلثًا", TimeWords.phrase(12, 39, true))
        assertEquals("الساعة التاسعة والنصف", TimeWords.phrase(9, 31, true))
        assertEquals("الساعة الثانية عشرة تمامًا", TimeWords.phrase(0, 1, true))
        assertEquals("الساعة الحادية عشرة والربع", TimeWords.phrase(23, 15, true))
    }

    private val defaults = LauncherSettings()
    private fun roundTrip(s: LauncherSettings) = settingsFromJson(JSONObject(settingsToJson(s).toString()))

    @Test fun classicStaysTheDefault() {
        assertEquals(HomeStyle.CLASSIC, defaults.homeStyle)
        assertEquals(HomeStyle.CLASSIC, settingsFromJson(JSONObject()).homeStyle)
    }

    @Test fun homeStyleSettingsRoundTrip() {
        val s = defaults.copy(
            homeStyle = HomeStyle.CUSTOM, modernFill = ModernFill.TODAY, modernCenter = ModernCenter.SPEED,
            favSide = FavSide.RIGHT, favCount = 12, modernPlaces = false, modernSuggest = false,
            modernReminders = false, modernCards = listOf(CardType.TRIP), edgeSwipe = false,
            internetBadge = false, customDriveLayout = true
        )
        assertEquals(s, roundTrip(s))
    }

    @Test fun noBottomCardsIsAllowed() {
        val s = defaults.copy(modernCards = emptyList())
        assertEquals(s, roundTrip(s))
    }

    @Test fun badHomeStyleValuesFallBack() {
        val o = settingsToJson(defaults)
            .put("homeStyle", "FUTURE")
            .put("favCount", 7)
            .put("favSide", "MIDDLE")
            .put("modernCards", JSONArray(listOf("MUSIC", "UFO", "MUSIC", "TRIP", "CAR", "PRAYER")))
        val s = settingsFromJson(o)
        assertEquals(HomeStyle.CLASSIC, s.homeStyle)
        assertEquals(defaults.favCount, s.favCount)
        assertEquals(FavSide.LEFT, s.favSide)
        assertEquals(listOf(CardType.MUSIC, CardType.TRIP, CardType.CAR), s.modernCards)
    }
}
