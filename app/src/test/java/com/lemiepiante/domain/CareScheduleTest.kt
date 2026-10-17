package com.lemiepiante.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CareScheduleTest {
    private val summerDay = LocalDate.of(2026, 6, 10)
    private val winterDay = LocalDate.of(2026, 12, 10)

    private fun water(last: LocalDate?, today: LocalDate, snooze: LocalDate? = null, summer: Int = 7, winter: Int = 14) =
        CareSchedule.status(CareType.WATER, summer, winter, last, snooze, today)

    @Test
    fun seasons() {
        assertEquals(Season.SUMMER, Seasons.of(LocalDate.of(2026, 10, 31)))
        assertEquals(Season.WINTER, Seasons.of(LocalDate.of(2026, 11, 1)))
        assertEquals(Season.WINTER, Seasons.of(LocalDate.of(2027, 2, 28)))
        assertEquals(Season.SUMMER, Seasons.of(LocalDate.of(2027, 3, 1)))
    }

    @Test
    fun usesSummerInterval() {
        val s = water(last = summerDay.minusDays(3), today = summerDay)
        assertEquals(7, s.intervalDays)
        assertEquals(4L, s.daysLeft)
        assertEquals(Urgency.OK, s.urgency)
    }

    @Test
    fun usesWinterInterval() {
        val s = water(last = winterDay.minusDays(3), today = winterDay)
        assertEquals(14, s.intervalDays)
        assertEquals(11L, s.daysLeft)
    }

    @Test
    fun urgencyLevels() {
        assertEquals(Urgency.SOON, water(summerDay.minusDays(5), summerDay).urgency)
        assertEquals(Urgency.TODAY, water(summerDay.minusDays(7), summerDay).urgency)
        val late = water(summerDay.minusDays(9), summerDay)
        assertEquals(Urgency.OVERDUE, late.urgency)
        assertEquals(-2L, late.daysLeft)
        assertTrue(late.isDue)
    }

    @Test
    fun neverDoneIsDueToday() {
        val s = water(last = null, today = summerDay)
        assertEquals(Urgency.NEVER_DONE, s.urgency)
        assertEquals(0L, s.daysLeft)
        assertTrue(s.isDue)
    }

    @Test
    fun zeroIntervalDisables() {
        val s = CareSchedule.status(CareType.FERTILIZE, 30, 0, winterDay.minusDays(60), null, winterDay)
        assertEquals(Urgency.DISABLED, s.urgency)
        assertNull(s.dueDate)
        assertFalse(s.isDue)
    }

    @Test
    fun snoozePostponesDueDate() {
        val s = water(summerDay.minusDays(8), summerDay, snooze = summerDay.plusDays(2))
        assertTrue(s.snoozed)
        assertEquals(2L, s.daysLeft)
        assertEquals(Urgency.SOON, s.urgency)
    }

    @Test
    fun snoozeBeforeNaturalDueIsIgnored() {
        val s = water(summerDay.minusDays(1), summerDay, snooze = summerDay.plusDays(2))
        assertFalse(s.snoozed)
        assertEquals(6L, s.daysLeft)
    }

    @Test
    fun snoozeAlsoWorksWhenNeverDone() {
        val s = water(null, summerDay, snooze = summerDay.plusDays(3))
        assertEquals(Urgency.OK, s.urgency)
        assertEquals(3L, s.daysLeft)
    }

    @Test
    fun sortKeyPrefersMostUrgent() {
        val ok = water(summerDay, summerDay)
        val late = water(summerDay.minusDays(10), summerDay)
        val never = water(null, summerDay)
        val off = CareSchedule.status(CareType.FERTILIZE, 0, 0, null, null, summerDay)
        assertEquals(-3L, CareSchedule.sortKey(listOf(ok, late)))
        assertEquals(Long.MIN_VALUE, CareSchedule.sortKey(listOf(ok, never)))
        assertEquals(Long.MAX_VALUE, CareSchedule.sortKey(listOf(off)))
    }

    @Test
    fun headlines() {
        assertEquals("Tra 4 giorni", water(summerDay.minusDays(3), summerDay).headline())
        assertEquals("Domani", water(summerDay.minusDays(6), summerDay).headline())
        assertEquals("Oggi", water(summerDay.minusDays(7), summerDay).headline())
        assertEquals("In ritardo di 1 giorno", water(summerDay.minusDays(8), summerDay).headline())
    }
}
