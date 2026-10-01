package com.classprep.junior.domain.planning

import com.classprep.junior.domain.model.DateTask
import com.classprep.junior.domain.model.IconKeys
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.LessonSlot
import com.classprep.junior.domain.model.ReusableItem
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.model.WeekConfig
import java.time.DayOfWeek
import java.time.LocalDate

/** Reusable item suggestions offered to parents. They are templates, never inserted automatically. */
object ItemSuggestions {
    data class Suggestion(val name: String, val iconKey: String, val category: ItemCategory)

    val all = listOf(
        Suggestion("Notebook", IconKeys.ITEM_NOTEBOOK, ItemCategory.BOOKS),
        Suggestion("Workbook", IconKeys.ITEM_WORKBOOK, ItemCategory.BOOKS),
        Suggestion("Pencil case", IconKeys.ITEM_PENCIL_CASE, ItemCategory.BOOKS),
        Suggestion("Water bottle", IconKeys.ITEM_BOTTLE, ItemCategory.OTHER),
        Suggestion("Lunchbox", IconKeys.ITEM_LUNCHBOX, ItemCategory.OTHER),
        Suggestion("School uniform", IconKeys.ITEM_UNIFORM, ItemCategory.CLOTHES),
        Suggestion("Sports clothes", IconKeys.ITEM_SPORTS_CLOTHES, ItemCategory.CLOTHES),
        Suggestion("Trainers", IconKeys.ITEM_SHOES, ItemCategory.CLOTHES),
        Suggestion("Art folder", IconKeys.ITEM_ART_FOLDER, ItemCategory.OTHER),
        Suggestion("Homework folder", IconKeys.ITEM_FOLDER, ItemCategory.BOOKS),
    )
}

/**
 * Clearly labelled sample week for "Explore an example". Lives only in memory: never written to the
 * database, never used for reminders or history.
 */
object ExampleData {
    private const val MATH = -1L
    private const val ENGLISH = -2L
    private const val SCIENCE = -3L
    private const val PE = -4L
    private const val ART = -5L

    private const val NOTEBOOK_MATH = -101L
    private const val NOTEBOOK_ENGLISH = -102L
    private const val WORKBOOK = -103L
    private const val RULER = -104L
    private const val PENCIL_CASE = -105L
    private const val SPORTS = -106L
    private const val TRAINERS = -107L
    private const val BOTTLE = -108L
    private const val ART_FOLDER = -109L
    private const val UNIFORM = -110L

    val config: WeekConfig = run {
        val school = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val subjects = listOf(
            Subject(MATH, "Mathematics", IconKeys.SUBJECT_MATH, 0),
            Subject(ENGLISH, "English", IconKeys.SUBJECT_ENGLISH, 1),
            Subject(SCIENCE, "Science", IconKeys.SUBJECT_SCIENCE, 2),
            Subject(PE, "Physical Education", IconKeys.SUBJECT_PE, 3),
            Subject(ART, "Art", IconKeys.SUBJECT_ART, 4),
        )
        val week = mapOf(
            DayOfWeek.MONDAY to listOf(MATH, ENGLISH, SCIENCE),
            DayOfWeek.TUESDAY to listOf(ENGLISH, MATH, PE, MATH),
            DayOfWeek.WEDNESDAY to listOf(SCIENCE, ART, ENGLISH),
            DayOfWeek.THURSDAY to listOf(MATH, PE, ENGLISH),
            DayOfWeek.FRIDAY to listOf(ART, MATH, SCIENCE),
        )
        val lessons = week.flatMap { (d, ids) -> ids.mapIndexed { i, s -> LessonSlot(d, s, i + 1) } }
        val items = listOf(
            ReusableItem(NOTEBOOK_MATH, "Notebook", "Blue cover", IconKeys.ITEM_NOTEBOOK, ItemCategory.BOOKS),
            ReusableItem(NOTEBOOK_ENGLISH, "Notebook", "Red cover", IconKeys.ITEM_NOTEBOOK, ItemCategory.BOOKS),
            ReusableItem(WORKBOOK, "Workbook", null, IconKeys.ITEM_WORKBOOK, ItemCategory.BOOKS),
            ReusableItem(RULER, "Ruler", null, IconKeys.ITEM_RULER, ItemCategory.BOOKS),
            ReusableItem(PENCIL_CASE, "Pencil case", null, IconKeys.ITEM_PENCIL_CASE, ItemCategory.BOOKS),
            ReusableItem(SPORTS, "Sports clothes", null, IconKeys.ITEM_SPORTS_CLOTHES, ItemCategory.CLOTHES),
            ReusableItem(TRAINERS, "Trainers", null, IconKeys.ITEM_SHOES, ItemCategory.CLOTHES),
            ReusableItem(BOTTLE, "Water bottle", null, IconKeys.ITEM_BOTTLE, ItemCategory.OTHER),
            ReusableItem(ART_FOLDER, "Art folder", null, IconKeys.ITEM_ART_FOLDER, ItemCategory.OTHER),
            ReusableItem(UNIFORM, "School uniform", null, IconKeys.ITEM_UNIFORM, ItemCategory.CLOTHES),
        )
        WeekConfig(
            schoolDays = school,
            subjects = subjects,
            lessons = lessons,
            items = items,
            subjectItems = mapOf(
                MATH to setOf(NOTEBOOK_MATH, WORKBOOK, RULER, PENCIL_CASE),
                ENGLISH to setOf(NOTEBOOK_ENGLISH, PENCIL_CASE),
                SCIENCE to setOf(WORKBOOK, PENCIL_CASE),
                PE to setOf(SPORTS, TRAINERS),
                ART to setOf(ART_FOLDER, PENCIL_CASE),
            ),
            weekdayItems = school.associateWith { setOf(UNIFORM, BOTTLE) },
        )
    }

    fun tasksFor(date: LocalDate): List<DateTask> = listOf(
        DateTask(-1000L - date.dayOfMonth, date, "Put the completed worksheet in your folder", null, IconKeys.ITEM_TASK, ItemCategory.HOMEWORK),
    )
}
