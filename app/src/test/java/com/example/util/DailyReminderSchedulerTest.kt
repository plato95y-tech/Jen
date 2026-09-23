package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DailyReminderSchedulerTest {

    @Test
    fun testFormat12HourEvening() {
        // 20:30 -> 08:30 م
        val formatted = DailyReminderScheduler.format12Hour(20, 30)
        assertEquals("08:30 م", formatted)
    }

    @Test
    fun testFormat12HourMorning() {
        // 09:05 -> 09:05 ص
        val formatted = DailyReminderScheduler.format12Hour(9, 5)
        assertEquals("09:05 ص", formatted)
    }

    @Test
    fun testFormat12HourMidnight() {
        // 00:00 -> 12:00 ص
        val formatted = DailyReminderScheduler.format12Hour(0, 0)
        assertEquals("12:00 ص", formatted)
    }

    @Test
    fun testFormat12HourNoon() {
        // 12:15 -> 12:15 م
        val formatted = DailyReminderScheduler.format12Hour(12, 15)
        assertEquals("12:15 م", formatted)
    }

    @Test
    fun testWeekDaysListContainsAllSevenDays() {
        assertEquals(7, DailyReminderScheduler.WEEK_DAYS.size)
        val calendarDays = DailyReminderScheduler.WEEK_DAYS.map { it.calendarDay }.toSet()
        assertEquals(7, calendarDays.size)
        assertTrue(calendarDays.contains(Calendar.SATURDAY))
        assertTrue(calendarDays.contains(Calendar.SUNDAY))
        assertTrue(calendarDays.contains(Calendar.MONDAY))
        assertTrue(calendarDays.contains(Calendar.TUESDAY))
        assertTrue(calendarDays.contains(Calendar.WEDNESDAY))
        assertTrue(calendarDays.contains(Calendar.THURSDAY))
        assertTrue(calendarDays.contains(Calendar.FRIDAY))
    }
}
