package com.carhome.launcher

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TripHistoryTest {

    private lateinit var savedTz: TimeZone
    @Before fun setUp() { savedTz = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh")) }
    @After fun tearDown() = TimeZone.setDefault(savedTz)

    private val day = 86_400_000L
    private fun at(y: Int, m: Int, d: Int, h: Int = 12) =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, 0, 0) }.timeInMillis

    private fun trip(end: Long, km: Double, min: Long) =
        PastTrip(startedAt = end - min * 60_000L, endedAt = end, distanceM = km * 1000, movingMs = min * 60_000L, maxMps = 30f, harshEvents = 0)

    private val now = at(2026, 11, 7, 22)
    private val trips = listOf(
        trip(at(2026, 10, 20), 100.0, 60),   // last month
        trip(at(2026, 11, 1), 40.0, 30),     // this month, 6 days ago
        trip(at(2026, 11, 6), 20.0, 20),     // this month, yesterday
        trip(at(2026, 11, 7, 9), 10.0, 15)   // today
    )

    @Test fun lastSevenDays() {
        val t = TripHistory.lastDays(trips, now, 7)
        assertEquals(3, t.trips)
        assertEquals(70_000.0, t.distanceM, 0.1)
        assertEquals(65 * 60_000L, t.movingMs)
    }

    @Test fun thisMonth() {
        val t = TripHistory.thisMonth(trips, now)
        assertEquals(3, t.trips)
        assertEquals(70_000.0, t.distanceM, 0.1)
    }

    @Test fun earlierInTheMonthButOverAWeekAgo() {
        val t = TripHistory.lastDays(trips + trip(at(2026, 11, 1, 1) - 3 * day, 5.0, 5), now, 7)
        assertEquals(3, t.trips)
    }

    @Test fun fuelCostFromDistance() {
        // 70 km at 10 l/100 km and 2.33 per litre = 16.31
        assertEquals(16.31, TripHistory.fuelCost(70_000.0, 10f, 2.33f), 0.01)
    }

    @Test fun newestFirstAndCapped() {
        val many = (1..400).map { trip(now - it * day, 1.0, 5) }
        val kept = TripHistory.add(many, trip(now, 3.0, 10))
        assertEquals(TripHistory.MAX, kept.size)
        assertEquals(now, kept.first().endedAt)
    }

    @Test fun shortHopsAreNotKept() {
        val kept = TripHistory.add(emptyList(), trip(now, 0.1, 1))
        assertEquals(0, kept.size)
    }

    @Test fun nothingYet() {
        val t = TripHistory.thisMonth(emptyList(), now)
        assertEquals(0, t.trips)
        assertEquals(0.0, t.distanceM, 0.0)
    }
}
