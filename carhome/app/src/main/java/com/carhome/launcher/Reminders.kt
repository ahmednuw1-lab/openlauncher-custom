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

    /** How far "Renewed" moves a reminder on: months for dates, km for distances. */
    fun renewMonths(t: ReminderType): Int = when (t) {
        ReminderType.LICENCE -> 60
        ReminderType.BATTERY -> 24
        else -> 12
    }

    fun renewKm(t: ReminderType): Double = when (t) {
        ReminderType.TYRES -> 40_000.0
        else -> 10_000.0
    }

    /**
     * The reminder after it was renewed: renewed early keeps its cycle (due date +1 period);
     * renewed late counts from today. By km: from the odometer now.
     */
    fun renewed(r: CarReminder, now: Long, odometerKm: Double): CarReminder {
        val today = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val date = r.dueDate?.let { due ->
            Calendar.getInstance().apply {
                timeInMillis = maxOf(due, today)
                add(Calendar.MONTH, renewMonths(r.type))
            }.timeInMillis
        }
        val km = r.dueKm?.let { odometerKm + renewKm(r.type) }
        return r.copy(dueDate = date, dueKm = km)
    }

    /** Local midnight of the given date. */
    fun dayStart(year: Int, month0: Int, day: Int): Long =
        Calendar.getInstance().apply { clear(); set(year, month0, day, 0, 0, 0) }.timeInMillis
}
