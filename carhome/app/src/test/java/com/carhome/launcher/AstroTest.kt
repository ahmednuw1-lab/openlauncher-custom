package com.carhome.launcher

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * Prayer times checked against an independent implementation (the "adhan"
 * library, Umm al-Qura method) for Saudi cities across the seasons.
 * Printed timetables differ by about a minute, so 2 minutes is allowed.
 */
class AstroTest {

    private lateinit var savedTz: TimeZone

    @Before fun setUp() {
        savedTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh"))
    }

    @After fun tearDown() = TimeZone.setDefault(savedTz)

    private data class City(val lat: Double, val lon: Double)
    private val hofuf = City(25.3833, 49.5864)
    private val riyadh = City(24.7136, 46.6753)
    private val makkah = City(21.4225, 39.8262)

    private fun day(y: Int, m: Int, d: Int) = Calendar.getInstance().apply {
        clear(); set(y, m - 1, d, 12, 0, 0)
    }

    private fun minutesOfDay(ms: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    private fun hm(s: String): Int = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    /** expected: fajr, sunrise, dhuhr, asr, maghrib, isha */
    private fun check(city: City, y: Int, m: Int, d: Int, vararg expected: String, ramadan: Boolean = false) {
        val p = Astro.prayerDay(day(y, m, d), city.lat, city.lon, PrayerMethod.UMM_AL_QURA, hanafi = false, ramadan = ramadan)
        val got = listOf(p.fajr, p.sunrise, p.dhuhr, p.asr, p.maghrib, p.isha).map(::minutesOfDay)
        val names = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
        expected.map(::hm).forEachIndexed { i, want ->
            assertTrue("${names[i]} on $y-$m-$d: got ${got[i] / 60}:${got[i] % 60}, want ${expected[i]}",
                abs(got[i] - want) <= 2)
        }
    }

    @Test fun hofufAutumn() = check(hofuf, 2026, 11, 7, "04:33", "05:53", "11:25", "14:34", "16:57", "18:27")
    @Test fun hofufSummer() = check(hofuf, 2026, 6, 21, "03:19", "04:52", "11:43", "15:06", "18:35", "20:05")
    @Test fun hofufWinter() = check(hofuf, 2026, 12, 21, "04:59", "06:23", "11:40", "14:37", "16:56", "18:26")
    @Test fun hofufSpring() = check(hofuf, 2027, 3, 20, "04:28", "05:46", "11:49", "15:16", "17:53", "19:23")
    @Test fun riyadhAutumn() = check(riyadh, 2026, 11, 7, "04:44", "06:04", "11:37", "14:47", "17:10", "18:40")
    @Test fun riyadhSummer() = check(riyadh, 2026, 6, 21, "03:33", "05:05", "11:55", "15:16", "18:45", "20:15")
    @Test fun makkahWinter() = check(makkah, 2026, 12, 21, "05:32", "06:54", "12:19", "15:23", "17:44", "19:14")
    @Test fun makkahSpring() = check(makkah, 2027, 3, 20, "05:09", "06:25", "12:28", "15:53", "18:32", "20:02")

    @Test fun ummAlQuraIshaIs90MinutesAfterMaghrib() {
        val p = Astro.prayerDay(day(2026, 11, 7), hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, false)
        assertEquals(90L * 60_000L, p.isha - p.maghrib)
    }

    @Test fun ummAlQuraIshaIs120MinutesAfterMaghribInRamadan() {
        val p = Astro.prayerDay(day(2027, 2, 20), hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, false, ramadan = true)
        assertEquals(120L * 60_000L, p.isha - p.maghrib)
    }

    @Test fun hanafiAsrIsLaterThanStandard() {
        val std = Astro.prayerDay(day(2026, 11, 7), hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, false)
        val han = Astro.prayerDay(day(2026, 11, 7), hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, true)
        assertTrue(han.asr - std.asr in 30 * 60_000L..90 * 60_000L)
    }

    @Test fun timesAreInOrder() {
        for (method in PrayerMethod.entries) {
            val p = Astro.prayerDay(day(2026, 11, 7), hofuf.lat, hofuf.lon, method, false)
            val all = listOf(p.fajr, p.sunrise, p.dhuhr, p.asr, p.maghrib, p.isha)
            assertEquals("order for $method", all.sorted(), all)
        }
    }

    @Test fun nextPrayerAfterIshaIsTomorrowsFajr() {
        val today = Astro.prayerDay(day(2026, 11, 7), hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, false)
        val next = Astro.nextPrayer(today.isha + 60_000L, hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, false, false)
        assertEquals(Prayer.FAJR, next.prayer)
        assertTrue(next.at > today.isha)
        assertTrue(next.at - today.fajr in 23 * 3_600_000L..25 * 3_600_000L)
    }

    @Test fun nextPrayerAtNoonIsDhuhrOrAsr() {
        val today = Astro.prayerDay(day(2026, 11, 7), hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, false)
        val next = Astro.nextPrayer(today.dhuhr - 60_000L, hofuf.lat, hofuf.lon, PrayerMethod.UMM_AL_QURA, false, false)
        assertEquals(Prayer.DHUHR, next.prayer)
        assertEquals(today.dhuhr, next.at)
    }

    @Test fun sameTimesWhateverTheHourTheyAreWorkedOut() {
        // On a daylight-saving change day the answer must not depend on when it is computed
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        fun at(h: Int, m: Int) = Calendar.getInstance().apply { clear(); set(2027, 2, 28, h, m, 0) }
        val early = Astro.prayerDay(at(0, 30), 51.5, -0.13, PrayerMethod.MWL, false)
        val late = Astro.prayerDay(at(12, 30), 51.5, -0.13, PrayerMethod.MWL, false)
        assertEquals(early, late)
        // And Dhuhr (solar noon in London, BST from 01:00) is about 13:05 local
        assertEquals((13 * 60 + 5).toDouble(), minutesOfDay(late.dhuhr).toDouble(), 3.0)
    }
}
