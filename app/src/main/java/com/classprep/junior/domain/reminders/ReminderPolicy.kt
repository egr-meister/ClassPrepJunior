package com.classprep.junior.domain.reminders

import com.classprep.junior.domain.model.PlanStatus
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure reminder rules. One local reminder on the evening before a school day; scheduled inexactly.
 * All times are in the device's current zone; callers recompute after time or zone changes.
 */
object ReminderPolicy {
    val DEFAULT_TIME: LocalTime = LocalTime.of(19, 0)
    val EARLIEST_TIME: LocalTime = LocalTime.of(16, 0)
    val LATEST_TIME: LocalTime = LocalTime.of(22, 0)

    /** A delayed delivery is still useful for this long after the chosen time (never past 23:30). */
    val GRACE: Duration = Duration.ofHours(3)
    private val HARD_CUTOFF: LocalTime = LocalTime.of(23, 30)

    fun clampTime(t: LocalTime): LocalTime = when {
        t.isBefore(EARLIEST_TIME) -> EARLIEST_TIME
        t.isAfter(LATEST_TIME) -> LATEST_TIME
        else -> t
    }

    /**
     * Next evening (date + time) to schedule, strictly after [now] and not before [notBeforeEvening],
     * whose following calendar day is a configured school day. Null if no school day is configured.
     */
    fun nextEvening(
        now: ZonedDateTime,
        time: LocalTime,
        schoolDays: Set<DayOfWeek>,
        notBeforeEvening: LocalDate? = null,
    ): ZonedDateTime? {
        if (schoolDays.isEmpty()) return null
        var evening = now.toLocalDate()
        if (notBeforeEvening != null && evening.isBefore(notBeforeEvening)) evening = notBeforeEvening
        repeat(15) {
            val at = evening.atTime(time).atZone(now.zone)
            if (at.isAfter(now) && evening.plusDays(1).dayOfWeek in schoolDays) return at
            evening = evening.plusDays(1)
        }
        return null
    }

    /** True when a delivery for [eveningDate] at [time] arrives too late to be useful (late-night catch-up). */
    fun isStale(now: ZonedDateTime, eveningDate: LocalDate, time: LocalTime): Boolean {
        if (now.toLocalDate() != eveningDate) return true
        val scheduled = eveningDate.atTime(time)
        val latest = minOf(scheduled.plus(GRACE), eveningDate.atTime(HARD_CUTOFF))
        return now.toLocalDateTime().isAfter(latest)
    }

    data class Eligibility(val eligible: Boolean, val reason: String)

    /**
     * Whether to post the reminder for [targetDate] (the calendar day after the evening).
     */
    fun eligibility(
        targetDate: LocalDate,
        schoolDays: Set<DayOfWeek>,
        checklistSize: Int,
        status: PlanStatus,
        alreadyNotifiedFor: LocalDate?,
    ): Eligibility = when {
        targetDate.dayOfWeek !in schoolDays -> Eligibility(false, "not a school day")
        checklistSize == 0 -> Eligibility(false, "no preparation items")
        status == PlanStatus.CONFIRMED -> Eligibility(false, "already confirmed ready")
        alreadyNotifiedFor == targetDate -> Eligibility(false, "already notified")
        else -> Eligibility(true, "eligible")
    }
}
