package com.classprep.junior.domain

import com.classprep.junior.domain.model.PlanStatus
import com.classprep.junior.domain.reminders.ReminderPolicy
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
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {
    private val zone = ZoneId.of("Europe/Kyiv")
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    private val seven = LocalTime.of(19, 0)
    private fun at(date: String, time: String) = ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), zone)

    @Test fun schedulesTonightBeforeSchoolDay() {
        // Monday 2026-10-05 at 10:00 → Monday 19:00 (Tuesday is a school day).
        val next = ReminderPolicy.nextEvening(at("2026-10-05", "10:00"), seven, weekdays)
        assertEquals(at("2026-10-05", "19:00"), next)
    }

    @Test fun skipsFridayAndSaturdayEvenings() {
        // Friday 20:00 → next evening before a school day is Sunday 19:00.
        val next = ReminderPolicy.nextEvening(at("2026-10-09", "20:00"), seven, weekdays)
        assertEquals(at("2026-10-11", "19:00"), next)
    }

    @Test fun afterDeliveryMovesToNextEvening() {
        val next = ReminderPolicy.nextEvening(at("2026-10-05", "19:01"), seven, weekdays, notBeforeEvening = LocalDate.parse("2026-10-06"))
        assertEquals(at("2026-10-06", "19:00"), next)
    }

    @Test fun nothingWithoutSchoolDays() {
        assertNull(ReminderPolicy.nextEvening(at("2026-10-05", "10:00"), seven, emptySet()))
    }

    @Test fun nextEveningUsesCurrentZoneAfterTimeZoneChange() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val now = ZonedDateTime.of(LocalDate.parse("2026-10-05"), LocalTime.of(10, 0), tokyo)
        val next = ReminderPolicy.nextEvening(now, seven, weekdays)!!
        assertEquals(tokyo, next.zone)
        assertEquals(LocalTime.of(19, 0), next.toLocalTime())
    }

    @Test fun staleDeliveryRejected() {
        val evening = LocalDate.parse("2026-10-05")
        assertFalse(ReminderPolicy.isStale(at("2026-10-05", "19:05"), evening, seven))
        assertFalse(ReminderPolicy.isStale(at("2026-10-05", "21:59"), evening, seven))
        assertTrue(ReminderPolicy.isStale(at("2026-10-05", "22:01"), evening, seven)) // > 3h grace
        assertTrue(ReminderPolicy.isStale(at("2026-10-06", "07:00"), evening, seven)) // next morning
        assertTrue(ReminderPolicy.isStale(at("2026-10-05", "23:45"), evening, LocalTime.of(22, 0))) // hard cutoff
    }

    @Test fun eligibilityRules() {
        val tue = LocalDate.parse("2026-10-06")
        val sat = LocalDate.parse("2026-10-10")
        assertTrue(ReminderPolicy.eligibility(tue, weekdays, 5, PlanStatus.OPEN, null).eligible)
        assertTrue(ReminderPolicy.eligibility(tue, weekdays, 5, PlanStatus.NEEDS_REVIEW, null).eligible)
        assertFalse(ReminderPolicy.eligibility(sat, weekdays, 5, PlanStatus.OPEN, null).eligible)
        assertFalse(ReminderPolicy.eligibility(tue, weekdays, 0, PlanStatus.OPEN, null).eligible)
        assertFalse(ReminderPolicy.eligibility(tue, weekdays, 5, PlanStatus.CONFIRMED, null).eligible)
        // Duplicate notification prevention.
        assertFalse(ReminderPolicy.eligibility(tue, weekdays, 5, PlanStatus.OPEN, tue).eligible)
    }

    @Test fun timeIsClampedToEvening() {
        assertEquals(ReminderPolicy.EARLIEST_TIME, ReminderPolicy.clampTime(LocalTime.of(9, 0)))
        assertEquals(ReminderPolicy.LATEST_TIME, ReminderPolicy.clampTime(LocalTime.of(23, 30)))
    }
}
