package com.carhome.launcher

import java.util.Calendar
import java.util.TimeZone

/** Car paperwork (dates) and maintenance (dates or kilometres). */
enum class ReminderType(val byKm: Boolean) {
    REGISTRATION(false),   // استمارة
    INSURANCE(false),
    INSPECTION(false),     // الفحص الدوري
    LICENCE(false),
    TYRES(true),
    BATTERY(false),
    SERVICE(true),
    CUSTOM(false)
}

/** One reminder: due on a date, at an odometer reading, or both (whichever comes first). */
data class CarReminder(
    val id: String,
    val type: ReminderType,
    val name: String = "",
    /** Local midnight of the due day, or null */
    val dueDate: Long? = null,
    /** Odometer reading in km, or null */
    val dueKm: Double? = null
)

/** A reminder that should be shown now. Negative values = overdue by that much. */
data class DueStatus(val reminder: CarReminder, val daysLeft: Int?, val kmLeft: Double?, val overdue: Boolean)

object Reminders {

    const val WARN_DAYS = 30
    const val WARN_KM = 500.0

    /** Whole calendar days from [now]'s date to [due]'s date (local time). */
    fun daysBetween(now: Long, due: Long): Int {
        fun dayNumber(ms: Long): Long {
            val off = TimeZone.getDefault().getOffset(ms)
            return Math.floorDiv(ms + off, 86_400_000L)
        }
        return (dayNumber(due) - dayNumber(now)).toInt()
    }

    /** Null while the reminder is still far off; otherwise how close (or late) it is. */
    fun status(r: CarReminder, now: Long, odometerKm: Double): DueStatus? {
        val days = r.dueDate?.let { daysBetween(now, it) }
        val km = r.dueKm?.let { it - odometerKm }
        val dateNear = days != null && days <= WARN_DAYS
        val kmNear = km != null && km <= WARN_KM
        if (!dateNear && !kmNear) return null
        val overdue = (days != null && days < 0) || (km != null && km < 0)
        return DueStatus(r, if (dateNear) days else null, if (kmNear) km else null, overdue)
    }

    /** All reminders to show now, the most urgent first; snoozed ones wait. */
    fun due(list: List<CarReminder>, now: Long, odometerKm: Double, snoozedUntil: Map<String, Long> = emptyMap()): List<DueStatus> =
        list.filter { (snoozedUntil[it.id] ?: 0L) <= now }
            .mapNotNull { status(it, now, odometerKm) }
            .sortedBy { st -> minOf(st.daysLeft?.toDouble() ?: Double.MAX_VALUE, (st.kmLeft ?: Double.MAX_VALUE) / 50.0) }

    /** Local midnight of the given date. */
    fun dayStart(year: Int, month0: Int, day: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month0, day, 0, 0, 0) }.timeInMillis
}
