package com.classprep.junior.domain

import com.classprep.junior.domain.model.DateTask
import com.classprep.junior.domain.model.IconKeys
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.LessonSlot
import com.classprep.junior.domain.model.ReusableItem
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.model.WeekConfig
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class FixedClock(var current: ZonedDateTime) : AppClock {
    override fun now(): ZonedDateTime = current
}

object Fx {
    val zone: ZoneId = ZoneId.of("Europe/Kyiv")
    const val MATH = 1L
    const val ENGLISH = 2L
    const val PE = 3L

    const val MATH_NOTEBOOK = 10L
    const val ENGLISH_NOTEBOOK = 11L // same display name as MATH_NOTEBOOK on purpose
    const val PENCIL_CASE = 12L
    const val RULER = 13L
    const val SPORTS = 14L
    const val UNIFORM = 15L

    fun item(id: Long, name: String, cat: ItemCategory = ItemCategory.BOOKS, note: String? = null) =
        ReusableItem(id, name, note, IconKeys.ITEM_NOTEBOOK, cat)

    fun lessons(day: DayOfWeek, vararg subjects: Long) = subjects.mapIndexed { i, s -> LessonSlot(day, s, i + 1) }

    /** Mon–Fri school; Tuesday has Maths twice and PE. */
    fun config(): WeekConfig = WeekConfig(
        schoolDays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
        subjects = listOf(
            Subject(MATH, "Mathematics", IconKeys.SUBJECT_MATH, 0),
            Subject(ENGLISH, "English", IconKeys.SUBJECT_ENGLISH, 1),
            Subject(PE, "Physical Education", IconKeys.SUBJECT_PE, 2),
        ),
        lessons = lessons(MONDAY, MATH, ENGLISH) +
            lessons(TUESDAY, MATH, PE, MATH, ENGLISH) +
            lessons(WEDNESDAY, ENGLISH) +
            lessons(THURSDAY, MATH) +
            lessons(FRIDAY, PE),
        items = listOf(
            item(MATH_NOTEBOOK, "Notebook", note = "blue"),
            item(ENGLISH_NOTEBOOK, "Notebook", note = "red"),
            item(PENCIL_CASE, "Pencil case"),
            item(RULER, "Ruler"),
            item(SPORTS, "Sports clothes", ItemCategory.CLOTHES),
            item(UNIFORM, "School uniform", ItemCategory.CLOTHES),
        ),
        subjectItems = mapOf(
            MATH to setOf(MATH_NOTEBOOK, PENCIL_CASE, RULER),
            ENGLISH to setOf(ENGLISH_NOTEBOOK, PENCIL_CASE),
            PE to setOf(SPORTS),
        ),
        weekdayItems = mapOf(TUESDAY to setOf(UNIFORM, SPORTS)),
    )

    fun task(id: Long, date: LocalDate, name: String = "Bring the project poster") =
        DateTask(id, date, name, null, IconKeys.ITEM_TASK, ItemCategory.HOMEWORK)

    /** 2026-10-05 is a Monday. */
    val MONDAY_DATE: LocalDate = LocalDate.of(2026, 10, 5)
    val TUESDAY_DATE: LocalDate = MONDAY_DATE.plusDays(1)
}
