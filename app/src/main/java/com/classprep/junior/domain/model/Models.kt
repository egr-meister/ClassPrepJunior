package com.classprep.junior.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** Display categories for checklist rows. Persisted by [key]; never by ordinal. */
enum class ItemCategory(val key: String, val label: String) {
    HOMEWORK("homework", "Homework and tasks"),
    BOOKS("books", "Books and stationery"),
    CLOTHES("clothes", "Clothes and sports"),
    OTHER("other", "Other belongings");

    companion object {
        fun fromKey(key: String?): ItemCategory = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

data class Subject(
    val id: Long,
    val name: String,
    val iconKey: String,
    val displayOrder: Int,
)

data class ReusableItem(
    val id: Long,
    val name: String,
    val note: String?,
    val iconKey: String,
    val category: ItemCategory,
)

data class LessonSlot(
    val weekday: DayOfWeek,
    val subjectId: Long,
    val position: Int,
)

data class DateTask(
    val id: Long,
    val targetDate: LocalDate,
    val name: String,
    val note: String?,
    val iconKey: String,
    val category: ItemCategory,
)

/** The complete recurring configuration (timetable + items). Date tasks are kept separately per date. */
data class WeekConfig(
    val schoolDays: Set<DayOfWeek>,
    val subjects: List<Subject>,
    val lessons: List<LessonSlot>,
    val items: List<ReusableItem>,
    val subjectItems: Map<Long, Set<Long>>,
    val weekdayItems: Map<DayOfWeek, Set<Long>>,
) {
    fun isSchoolDay(day: DayOfWeek) = day in schoolDays
    fun lessonsFor(day: DayOfWeek): List<LessonSlot> =
        lessons.filter { it.weekday == day }.sortedBy { it.position }
    fun subject(id: Long): Subject? = subjects.firstOrNull { it.id == id }
    fun item(id: Long): ReusableItem? = items.firstOrNull { it.id == id }

    companion object {
        val EMPTY = WeekConfig(emptySet(), emptyList(), emptyList(), emptyList(), emptyMap(), emptyMap())
    }
}

/** Where a checklist row comes from. Persisted by [key]. */
enum class SourceType(val key: String) {
    ITEM("item"),
    TASK("task");

    companion object {
        fun fromKey(key: String): SourceType = entries.first { it.key == key }
    }
}

/** Stable identity of a checklist row: reusable item id or date task id. Never derived from names. */
data class ItemKey(val sourceType: SourceType, val sourceId: Long) {
    override fun toString() = "${sourceType.key}:$sourceId"
}

data class PlannedLesson(
    val position: Int,
    val subjectId: Long,
    val subjectName: String,
    val iconKey: String,
)

data class ChecklistEntry(
    val key: ItemKey,
    val name: String,
    val note: String?,
    val iconKey: String,
    val category: ItemCategory,
    /** Subject names in first-lesson order; empty for weekday-only items and tasks. */
    val subjectLabels: List<String>,
    val subjectIds: Set<Long>,
    /** True when the item is a weekday ("For the day") item. */
    val forTheDay: Boolean,
) {
    val isTask get() = key.sourceType == SourceType.TASK
}

/** Result of checklist generation for one actual date. */
data class GeneratedPlan(
    val date: LocalDate,
    val isSchoolDay: Boolean,
    val lessons: List<PlannedLesson>,
    val entries: List<ChecklistEntry>,
) {
    val overLimit: Boolean get() = entries.size > com.classprep.junior.domain.planning.Limits.MAX_CHECKLIST_ENTRIES
    val isEmpty: Boolean get() = entries.isEmpty()
}

data class ItemState(val ready: Boolean, val updatedAt: Instant)

/** Persisted state of a preparation plan for a date. */
data class PlanState(
    val id: Long,
    val date: LocalDate,
    val revision: Int,
    val signature: String,
    val confirmedRevision: Int?,
    val confirmedAt: Instant?,
    val states: Map<ItemKey, ItemState>,
)

enum class PlanStatus { OPEN, CONFIRMED, NEEDS_REVIEW }

data class HistoryLessonSnapshot(val position: Int, val subjectName: String, val iconKey: String)

data class HistoryItemSnapshot(
    val name: String,
    val note: String?,
    val category: ItemCategory,
    val iconKey: String,
    val subjectLabels: String,
)

data class HistorySnapshot(
    val targetDate: LocalDate,
    val confirmedAt: Instant,
    val planRevision: Int,
    val lessons: List<HistoryLessonSnapshot>,
    val items: List<HistoryItemSnapshot>,
) {
    val itemCount get() = items.size
}

data class HistoryEntry(
    val id: Long,
    val targetDate: LocalDate,
    val confirmedAt: Instant,
    val itemCount: Int,
    val planRevision: Int,
)
