package com.classprep.junior.domain.planning

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Pure date rules. `today` always comes from an injected clock in the device's current time zone. */
object DateSelection {

    fun defaultDate(today: LocalDate): LocalDate = today.plusDays(1)

    fun isTomorrow(date: LocalDate, today: LocalDate) = date == today.plusDays(1)

    /** Dates that can be prepared/edited: today through today + 14 days. */
    fun isEditable(date: LocalDate, today: LocalDate): Boolean =
        !date.isBefore(today) && !date.isAfter(today.plusDays(Limits.PLANNING_DAYS_AHEAD))

    fun clampToEditable(date: LocalDate, today: LocalDate): LocalDate = when {
        date.isBefore(today) -> today
        date.isAfter(today.plusDays(Limits.PLANNING_DAYS_AHEAD)) -> today.plusDays(Limits.PLANNING_DAYS_AHEAD)
        else -> date
    }

    /**
     * Next enabled school day strictly after [after], within the editable window.
     * Returns null when no school weekday is configured or none falls inside the window.
     */
    fun nextSchoolDay(after: LocalDate, today: LocalDate, schoolDays: Set<DayOfWeek>): LocalDate? {
        if (schoolDays.isEmpty()) return null
        var d = after.plusDays(1)
        val last = today.plusDays(Limits.PLANNING_DAYS_AHEAD)
        while (!d.isAfter(last)) {
            if (d.dayOfWeek in schoolDays) return d
            d = d.plusDays(1)
        }
        return null
    }

    /** Monday of the calendar week containing [date]. */
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekDays(weekStart: LocalDate): List<LocalDate> = (0L..6L).map { weekStart.plusDays(it) }

    fun canGoToPreviousWeek(weekStart: LocalDate, today: LocalDate) = weekStart.isAfter(weekStart(today))

    fun canGoToNextWeek(weekStart: LocalDate, today: LocalDate) =
        !weekStart.plusDays(7).isAfter(today.plusDays(Limits.PLANNING_DAYS_AHEAD))

    fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    fun shortDayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    fun longDate(date: LocalDate): String =
        "${dayName(date.dayOfWeek)}, ${date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${date.dayOfMonth}"

    /** Header context line for the planner. */
    fun headerContext(date: LocalDate, today: LocalDate, isSchoolDay: Boolean): String {
        val tomorrow = isTomorrow(date, today)
        return when {
            !isSchoolDay && tomorrow -> "No school planned for tomorrow."
            !isSchoolDay -> "No school planned."
            tomorrow -> "Tomorrow — ${dayName(date.dayOfWeek)}."
            date == today -> "Today — ${dayName(date.dayOfWeek)}."
            else -> "Preparing for ${dayName(date.dayOfWeek)}."
        }
    }

    /** Title for the confirmation screen: "Tomorrow ready" only when the date really is tomorrow. */
    fun readyTitle(date: LocalDate, today: LocalDate): String =
        if (isTomorrow(date, today)) "Tomorrow ready" else "Ready for ${dayName(date.dayOfWeek)}"
}
