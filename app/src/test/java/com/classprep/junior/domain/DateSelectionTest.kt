package com.classprep.junior.domain

import com.classprep.junior.domain.planning.DateSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class DateSelectionTest {
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    @Test fun defaultIsTomorrow() {
        assertEquals(LocalDate.of(2026, 10, 2), DateSelection.defaultDate(LocalDate.of(2026, 10, 1)))
    }

    @Test fun fridayDoesNotLabelMondayAsTomorrow() {
        val friday = LocalDate.of(2026, 10, 2)
        val saturday = friday.plusDays(1)
        assertEquals("No school planned for tomorrow.", DateSelection.headerContext(saturday, friday, isSchoolDay = false))
        val next = DateSelection.nextSchoolDay(saturday, friday, weekdays)
        assertEquals(LocalDate.of(2026, 10, 5), next)
        assertEquals("Preparing for Monday.", DateSelection.headerContext(next!!, friday, isSchoolDay = true))
        assertFalse(DateSelection.isTomorrow(next, friday))
        assertEquals("Ready for Monday", DateSelection.readyTitle(next, friday))
    }

    @Test fun tomorrowHeaderAndReadyTitle() {
        val mon = LocalDate.of(2026, 10, 5)
        assertEquals("Tomorrow — Tuesday.", DateSelection.headerContext(mon.plusDays(1), mon, true))
        assertEquals("Tomorrow ready", DateSelection.readyTitle(mon.plusDays(1), mon))
    }

    @Test fun nextSchoolDayAcrossYearBoundary() {
        val today = LocalDate.of(2026, 12, 31) // Thursday
        val after = today.plusDays(1) // Friday Jan 1 2027 — suppose it's a holiday: search after it
        assertEquals(LocalDate.of(2027, 1, 4), DateSelection.nextSchoolDay(after, today, setOf(MONDAY)))
    }

    @Test fun nextSchoolDayNullWhenNothingConfiguredOrOutOfWindow() {
        val today = LocalDate.of(2026, 10, 1)
        assertNull(DateSelection.nextSchoolDay(today, today, emptySet()))
        // Only Monday configured and the search starts at the edge of the 14-day window.
        assertNull(DateSelection.nextSchoolDay(today.plusDays(14), today, setOf(MONDAY)))
    }

    @Test fun weekBoundaries() {
        val sunday = LocalDate.of(2027, 1, 3)
        assertEquals(LocalDate.of(2026, 12, 28), DateSelection.weekStart(sunday))
        val days = DateSelection.weekDays(DateSelection.weekStart(sunday))
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2027, 1, 3), days.last())
    }

    @Test fun editableWindowIsTodayThrough14Days() {
        val today = LocalDate.of(2026, 10, 1)
        assertTrue(DateSelection.isEditable(today, today))
        assertTrue(DateSelection.isEditable(today.plusDays(14), today))
        assertFalse(DateSelection.isEditable(today.plusDays(15), today))
        assertFalse(DateSelection.isEditable(today.minusDays(1), today))
    }

    @Test fun weekNavigationLimits() {
        val today = LocalDate.of(2026, 10, 1)
        val ws = DateSelection.weekStart(today)
        assertFalse(DateSelection.canGoToPreviousWeek(ws, today))
        assertTrue(DateSelection.canGoToNextWeek(ws, today))
        assertFalse(DateSelection.canGoToNextWeek(ws.plusWeeks(2), today))
    }
}
