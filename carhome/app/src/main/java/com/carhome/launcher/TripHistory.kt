package com.carhome.launcher

import java.util.Calendar

/** A finished trip, kept for the trip history. */
data class PastTrip(
    val startedAt: Long,
    val endedAt: Long,
    val distanceM: Double,
    val movingMs: Long,
    val maxMps: Float,
    val harshEvents: Int
) {
    val avgMps: Float get() = if (movingMs > 0) (distanceM / (movingMs / 1000.0)).toFloat() else 0f
}

/** Totals for a period. */
data class TripTotals(val trips: Int, val distanceM: Double, val movingMs: Long)

/** Trip history rules (no Android, so they can be unit-tested). */
object TripHistory {

    /** How many trips are kept (newest first). */
    const val MAX = 300

    /** Trips under this are not kept (moving the car in the car park). */
    private const val MIN_M = 200.0

    /** Adds [t] at the front; returns the list unchanged for a very short trip. */
    fun add(history: List<PastTrip>, t: PastTrip): List<PastTrip> =
        if (t.distanceM < MIN_M) history else (listOf(t) + history).take(MAX)

    fun totals(trips: List<PastTrip>, fromMs: Long, toMs: Long): TripTotals {
        val sel = trips.filter { it.endedAt in fromMs..toMs }
        return TripTotals(sel.size, sel.sumOf { it.distanceM }, sel.sumOf { it.movingMs })
    }

    /** The last [days] days up to [now]. */
    fun lastDays(trips: List<PastTrip>, now: Long, days: Int): TripTotals =
        totals(trips, now - days * 86_400_000L, now)

    /** From the 1st of this month (local time) up to [now]. */
    fun thisMonth(trips: List<PastTrip>, now: Long): TripTotals {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return totals(trips, start, now)
    }

    /** Fuel cost for a distance at [litresPer100km] and [pricePerLitre]. */
    fun fuelCost(distanceM: Double, litresPer100km: Float, pricePerLitre: Float): Double =
        distanceM / 100_000.0 * litresPer100km * pricePerLitre
}
