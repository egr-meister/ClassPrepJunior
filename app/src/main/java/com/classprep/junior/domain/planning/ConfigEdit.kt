package com.classprep.junior.domain.planning

import com.classprep.junior.domain.model.LessonSlot
import com.classprep.junior.domain.model.ReusableItem
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.model.WeekConfig
import java.time.DayOfWeek

/**
 * A parent configuration change. [applyTo] is the pure in-memory equivalent of what the repository
 * persists, used to preview the impact on confirmed plans before saving and for unit tests.
 * New entities use id = 0 in previews (they cannot affect existing plans until linked).
 */
sealed interface ConfigEdit {
    fun applyTo(config: WeekConfig): WeekConfig

    data class UpsertSubject(val subject: Subject) : ConfigEdit {
        override fun applyTo(config: WeekConfig) = config.copy(
            subjects = config.subjects.filterNot { it.id == subject.id } + subject,
        )
    }

    data class DeleteSubject(val subjectId: Long) : ConfigEdit {
        override fun applyTo(config: WeekConfig) = config.copy(
            subjects = config.subjects.filterNot { it.id == subjectId },
            lessons = renumber(config.lessons.filterNot { it.subjectId == subjectId }),
            subjectItems = config.subjectItems - subjectId,
        )
    }

    /** Replaces one weekday's school flag and ordered lessons. A non-school day loses lessons and weekday items. */
    data class SetWeekday(val day: DayOfWeek, val isSchoolDay: Boolean, val subjectIds: List<Long>) : ConfigEdit {
        override fun applyTo(config: WeekConfig): WeekConfig {
            val others = config.lessons.filterNot { it.weekday == day }
            val mine = if (isSchoolDay) subjectIds.mapIndexed { i, s -> LessonSlot(day, s, i + 1) } else emptyList()
            return config.copy(
                schoolDays = if (isSchoolDay) config.schoolDays + day else config.schoolDays - day,
                lessons = others + mine,
                weekdayItems = if (isSchoolDay) config.weekdayItems else config.weekdayItems - day,
            )
        }
    }

    /** Setup step 1: sets the school weekday flags; days switched off lose lessons and weekday items. */
    data class SetSchoolDays(val days: Set<DayOfWeek>) : ConfigEdit {
        override fun applyTo(config: WeekConfig): WeekConfig {
            val removed = config.schoolDays - days
            return config.copy(
                schoolDays = days,
                lessons = config.lessons.filterNot { it.weekday in removed },
                weekdayItems = config.weekdayItems.filterKeys { it !in removed },
            )
        }
    }

    data class UpsertItem(val item: ReusableItem) : ConfigEdit {
        override fun applyTo(config: WeekConfig) = config.copy(
            items = config.items.filterNot { it.id == item.id } + item,
        )
    }

    data class DeleteItem(val itemId: Long) : ConfigEdit {
        override fun applyTo(config: WeekConfig) = config.copy(
            items = config.items.filterNot { it.id == itemId },
            subjectItems = config.subjectItems.mapValues { it.value - itemId },
            weekdayItems = config.weekdayItems.mapValues { it.value - itemId },
        )
    }

    data class SetSubjectItems(val subjectId: Long, val itemIds: Set<Long>) : ConfigEdit {
        override fun applyTo(config: WeekConfig) = config.copy(subjectItems = config.subjectItems + (subjectId to itemIds))
    }

    data class SetWeekdayItems(val day: DayOfWeek, val itemIds: Set<Long>) : ConfigEdit {
        override fun applyTo(config: WeekConfig) = config.copy(weekdayItems = config.weekdayItems + (day to itemIds))
    }

    companion object {
        fun renumber(lessons: List<LessonSlot>): List<LessonSlot> =
            lessons.groupBy { it.weekday }.flatMap { (_, slots) ->
                slots.sortedBy { it.position }.mapIndexed { i, s -> s.copy(position = i + 1) }
            }
    }
}
