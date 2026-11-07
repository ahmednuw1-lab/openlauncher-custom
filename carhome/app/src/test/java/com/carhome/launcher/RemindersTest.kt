package com.carhome.launcher

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RemindersTest {

    // Saudi time before the test values below are worked out (the build machine runs on UTC)
    companion object {
        private lateinit var savedTz: TimeZone
        @JvmStatic @BeforeClass fun setUp() { savedTz = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh")) }
        @JvmStatic @AfterClass fun tearDown() = TimeZone.setDefault(savedTz)
    }

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, 0, 0) }.timeInMillis

    private val now = at(2026, 11, 7, 23)

    private fun date(type: ReminderType, y: Int, m: Int, d: Int) = CarReminder("r1", type, dueDate = at(y, m, d, 0))
    private fun km(type: ReminderType, dueKm: Double) = CarReminder("r2", type, dueKm = dueKm)

    @Test fun farAwayIsQuiet() = assertNull(Reminders.status(date(ReminderType.INSURANCE, 2027, 3, 1), now, 0.0))

    @Test fun withinThirtyDaysWarns() {
        val st = Reminders.status(date(ReminderType.INSURANCE, 2026, 11, 19), now, 0.0)!!
        assertEquals(12, st.daysLeft)
        assertFalse(st.overdue)
    }

    @Test fun todayIsZeroDays() = assertEquals(0, Reminders.status(date(ReminderType.REGISTRATION, 2026, 11, 7), now, 0.0)!!.daysLeft)

    @Test fun lateNightCountsCalendarDays() {
        // 23:00 on the 7th, due on the 8th = 1 day, not 0
        assertEquals(1, Reminders.status(date(ReminderType.LICENCE, 2026, 11, 8), now, 0.0)!!.daysLeft)
    }

    @Test fun pastDateIsOverdue() {
        val st = Reminders.status(date(ReminderType.INSPECTION, 2026, 11, 4), now, 0.0)!!
        assertEquals(-3, st.daysLeft)
        assertTrue(st.overdue)
    }

    @Test fun kilometresWarnWithin500() {
        assertNull(Reminders.status(km(ReminderType.TYRES, 60_000.0), now, 59_000.0))
        val st = Reminders.status(km(ReminderType.TYRES, 60_000.0), now, 59_700.0)!!
        assertEquals(300.0, st.kmLeft!!, 0.1)
        assertFalse(st.overdue)
    }

    @Test fun kilometresPastAreOverdue() {
        val st = Reminders.status(km(ReminderType.SERVICE, 60_000.0), now, 60_120.0)!!
        assertTrue(st.overdue)
        assertEquals(-120.0, st.kmLeft!!, 0.1)
    }

    @Test fun whicheverComesFirst() {
        // Due by date or by km: the km side is already close
        val r = CarReminder("r3", ReminderType.SERVICE, dueDate = at(2027, 6, 1), dueKm = 60_000.0)
        assertEquals(200.0, Reminders.status(r, now, 59_800.0)!!.kmLeft!!, 0.1)
    }

    @Test fun nothingSetIsQuiet() = assertNull(Reminders.status(CarReminder("r4", ReminderType.CUSTOM), now, 1000.0))

    @Test fun mostUrgentFirst() {
        val list = listOf(
            date(ReminderType.INSURANCE, 2026, 11, 25).copy(id = "a"),
            date(ReminderType.INSPECTION, 2026, 11, 4).copy(id = "b"),
            date(ReminderType.LICENCE, 2026, 11, 10).copy(id = "c")
        )
        assertEquals(listOf("b", "c", "a"), Reminders.due(list, now, 0.0).map { it.reminder.id })
    }

    @Test fun snoozedOnesWait() {
        val r = date(ReminderType.INSURANCE, 2026, 11, 19)
        assertTrue(Reminders.due(listOf(r), now, 0.0, snoozedUntil = mapOf("r1" to now + 1000)).isEmpty())
        assertEquals(1, Reminders.due(listOf(r), now, 0.0, snoozedUntil = mapOf("r1" to now - 1000)).size)
    }

    @Test fun renewingEarlyKeepsTheYearlyCycle() {
        // Istimara due 26 Nov 2026, renewed on 7 Nov: next due 26 Nov 2027
        val r = Reminders.renewed(date(ReminderType.REGISTRATION, 2026, 11, 26), now, 0.0)
        assertEquals(at(2027, 11, 26, 0), r.dueDate)
    }

    @Test fun renewingLateCountsFromToday() {
        val r = Reminders.renewed(date(ReminderType.INSURANCE, 2026, 10, 1), now, 0.0)
        assertEquals(at(2027, 11, 7, 0), r.dueDate)
    }

    @Test fun renewingByKmCountsFromTheOdometer() {
        assertEquals(10_000.0 + 40_000.0, Reminders.renewed(km(ReminderType.TYRES, 10_300.0), now, 10_000.0).dueKm!!, 0.01)
        assertEquals(10_000.0 + 10_000.0, Reminders.renewed(km(ReminderType.SERVICE, 9_900.0), now, 10_000.0).dueKm!!, 0.01)
    }

    @Test fun renewedIsQuietAgain() {
        val r = Reminders.renewed(date(ReminderType.INSPECTION, 2026, 11, 20), now, 0.0)
        assertNull(Reminders.status(r, now, 0.0))
        assertEquals("r1", r.id)
    }
}
