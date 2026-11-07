package com.carhome.launcher

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

enum class Prayer { FAJR, DHUHR, ASR, MAGHRIB, ISHA }

/** One day's times as epoch milliseconds (local device time zone). */
data class PrayerDay(
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long
) {
    fun timeOf(p: Prayer): Long = when (p) {
        Prayer.FAJR -> fajr
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}

data class NextPrayer(val prayer: Prayer, val at: Long)

/**
 * Offline astronomy: prayer times (same maths as the widely used PrayTimes
 * method) and sunrise/sunset. Accurate to about a minute; no internet needed.
 */
object Astro {

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun dsin(d: Double) = sin(rad(d))
    private fun dcos(d: Double) = cos(rad(d))
    private fun dtan(d: Double) = tan(rad(d))
    private fun darcsin(x: Double) = deg(asin(x))
    private fun darccos(x: Double) = deg(acos(x))
    private fun darctan2(y: Double, x: Double) = deg(atan2(y, x))
    private fun darccot(x: Double) = deg(atan(1.0 / x))
    private fun fixAngle(a: Double) = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double) = a - 24.0 * floor(a / 24.0)

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Sun declination and equation of time for a Julian date. */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = darcsin(dsin(e) * dsin(l))
        return decl to eqt
    }

    private class Ctx(val jDate: Double, val lat: Double)

    private fun midDay(c: Ctx, t: Double): Double = fixHour(12 - sunPosition(c.jDate + t).second)

    private fun sunAngleTime(c: Ctx, angle: Double, t: Double, ccw: Boolean): Double {
        val decl = sunPosition(c.jDate + t).first
        val noon = midDay(c, t)
        val cosv = ((-dsin(angle) - dsin(decl) * dsin(c.lat)) / (dcos(decl) * dcos(c.lat))).coerceIn(-1.0, 1.0)
        val v = darccos(cosv) / 15.0
        return noon + if (ccw) -v else v
    }

    private fun asrTime(c: Ctx, factor: Double, t: Double): Double {
        val decl = sunPosition(c.jDate + t).first
        val angle = -darccot(factor + dtan(abs(c.lat - decl)))
        return sunAngleTime(c, angle, t, ccw = false)
    }

    private data class Angles(val fajr: Double, val isha: Double?, val ishaMinutes: Int)

    private fun angles(m: PrayerMethod): Angles = when (m) {
        PrayerMethod.UMM_AL_QURA -> Angles(18.5, null, 90)
        PrayerMethod.MWL -> Angles(18.0, 17.0, 0)
        PrayerMethod.EGYPT -> Angles(19.5, 17.5, 0)
        PrayerMethod.KARACHI -> Angles(18.0, 18.0, 0)
    }

    /** Hours (universal time) for fajr, sunrise, dhuhr, asr, sunset, isha-by-angle. */
    private fun dayHours(
        cal: Calendar, lat: Double, lon: Double, method: PrayerMethod, hanafi: Boolean
    ): DoubleArray {
        val jd = julian(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val c = Ctx(jd - lon / (15.0 * 24.0), lat)
        val a = angles(method)
        val factor = if (hanafi) 2.0 else 1.0
        // Two passes: the second uses the first pass's times for the sun position
        var t = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0)
        repeat(2) {
            val p = DoubleArray(6) { i -> t[i] / 24.0 }
            t = doubleArrayOf(
                sunAngleTime(c, a.fajr, p[0], ccw = true),
                sunAngleTime(c, 0.833, p[1], ccw = true),
                midDay(c, p[2]),
                asrTime(c, factor, p[3]),
                sunAngleTime(c, 0.833, p[4], ccw = false),
                sunAngleTime(c, a.isha ?: 0.833, p[5], ccw = false)
            )
        }
        // Universal time; added to UTC midnight, so daylight-saving changes can't shift it
        return DoubleArray(6) { i -> t[i] - lon / 15.0 }
    }

    /** UTC midnight of the calendar's own (local) date. */
    private fun utcMidnight(cal: Calendar): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis

    private fun at(base: Long, hours: Double): Long = base + (hours * 3_600_000.0).toLong()

    fun prayerDay(
        date: Calendar, lat: Double, lon: Double, method: PrayerMethod, hanafi: Boolean, ramadan: Boolean = false
    ): PrayerDay {
        val h = dayHours(date, lat, lon, method, hanafi)
        val base = utcMidnight(date)
        val maghrib = at(base, h[4])
        val a = angles(method)
        val isha = if (a.isha == null) {
            // Umm al-Qura: fixed 90 minutes after Maghrib, 120 in Ramadan
            maghrib + (if (ramadan) 120 else a.ishaMinutes) * 60_000L
        } else at(base, h[5])
        // Round to the minute, as printed timetables do
        fun r(ms: Long) = ((ms + 30_000L) / 60_000L) * 60_000L
        return PrayerDay(r(at(base, h[0])), r(at(base, h[1])), r(at(base, h[2])), r(at(base, h[3])), r(maghrib), r(isha))
    }

    /** Next prayer at or after [now] (looks into tomorrow after Isha). */
    fun nextPrayer(now: Long, lat: Double, lon: Double, method: PrayerMethod, hanafi: Boolean, ramadan: Boolean): NextPrayer {
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val d = prayerDay(today, lat, lon, method, hanafi, ramadan)
        Prayer.entries.forEach { p -> if (d.timeOf(p) >= now) return NextPrayer(p, d.timeOf(p)) }
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        return NextPrayer(Prayer.FAJR, prayerDay(tomorrow, lat, lon, method, hanafi, ramadan).fajr)
    }

    /** Today's sunrise and sunset (epoch ms) for night mode and the living sky. */
    fun sunTimes(now: Long, lat: Double, lon: Double): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val h = dayHours(cal, lat, lon, PrayerMethod.MWL, false)
        val base = utcMidnight(cal)
        return at(base, h[1]) to at(base, h[4])
    }

    /** Hijri (Umm al-Qura) date text, e.g. "16 جمادى الأولى 1448 هـ". Null if the device lacks ICU. */
    fun hijriDate(now: Long, locale: Locale): String? = runCatching {
        val lang = if (locale.language == "ar") "ar" else "en"
        val fmt = android.icu.text.SimpleDateFormat(
            "d MMMM y", android.icu.util.ULocale("$lang@calendar=islamic-umalqura;numbers=latn")
        )
        fmt.format(Date(now)) + if (lang == "ar") " هـ" else " AH"
    }.getOrNull()

    /** True during Ramadan (Umm al-Qura calendar). */
    fun isRamadan(now: Long): Boolean = runCatching {
        val cal = android.icu.util.IslamicCalendar()
        cal.calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        cal.timeInMillis = now
        cal.get(android.icu.util.Calendar.MONTH) == 8   // 0-based: Ramadan is the 9th month
    }.getOrDefault(false)
}
