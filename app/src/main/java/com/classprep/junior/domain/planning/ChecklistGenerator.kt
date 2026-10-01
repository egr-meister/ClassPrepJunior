package com.classprep.junior.domain.planning

import com.classprep.junior.domain.model.ChecklistEntry
import com.classprep.junior.domain.model.DateTask
import com.classprep.junior.domain.model.GeneratedPlan
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.PlannedLesson
import com.classprep.junior.domain.model.SourceType
import com.classprep.junior.domain.model.WeekConfig
import java.time.LocalDate

/**
 * Builds the effective checklist for one actual date.
 *
 * - Subject items are included once per item id, even when the subject repeats or several subjects share the item.
 * - Items are merged by stable id only — never by name.
 * - Weekday items are marked "For the day".
 * - Date-specific tasks are always included for their date.
 * - Nothing is ever dropped; [GeneratedPlan.overLimit] reports when the daily limit is exceeded.
 */
object ChecklistGenerator {

    fun generate(date: LocalDate, config: WeekConfig, tasks: List<DateTask>): GeneratedPlan {
        val day = date.dayOfWeek
        val school = config.isSchoolDay(day)
        val lessons = if (school) {
            config.lessonsFor(day).mapNotNull { slot ->
                config.subject(slot.subjectId)?.let { PlannedLesson(slot.position, it.id, it.name, it.iconKey) }
            }.sortedBy { it.position }
        } else {
            emptyList()
        }

        // itemId -> ordered subject ids (first lesson order, de-duplicated)
        val subjectsByItem = LinkedHashMap<Long, LinkedHashSet<Long>>()
        for (lesson in lessons) {
            val itemIds = config.subjectItems[lesson.subjectId].orEmpty()
            for (itemId in itemIds.sorted()) {
                subjectsByItem.getOrPut(itemId) { LinkedHashSet() }.add(lesson.subjectId)
            }
        }
        val weekdayItemIds = if (school) config.weekdayItems[day].orEmpty() else emptySet()

        val entries = ArrayList<ChecklistEntry>()
        val seen = HashSet<Long>()
        fun addItem(itemId: Long) {
            if (!seen.add(itemId)) return
            val item = config.item(itemId) ?: return
            val subjectIds = subjectsByItem[itemId].orEmpty()
            entries += ChecklistEntry(
                key = ItemKey(SourceType.ITEM, item.id),
                name = item.name,
                note = item.note,
                iconKey = item.iconKey,
                category = item.category,
                subjectLabels = subjectIds.mapNotNull { config.subject(it)?.name },
                subjectIds = subjectIds.toSet(),
                forTheDay = itemId in weekdayItemIds,
            )
        }
        subjectsByItem.keys.forEach(::addItem)
        weekdayItemIds.sorted().forEach(::addItem)

        tasks.filter { it.targetDate == date }.sortedBy { it.id }.forEach { task ->
            entries += ChecklistEntry(
                key = ItemKey(SourceType.TASK, task.id),
                name = task.name,
                note = task.note,
                iconKey = task.iconKey,
                category = task.category,
                subjectLabels = emptyList(),
                subjectIds = emptySet(),
                forTheDay = false,
            )
        }
        return GeneratedPlan(date, school, lessons, entries)
    }

    /** Count of related checklist rows per subject, for lesson rows. */
    fun countForSubject(plan: GeneratedPlan, subjectId: Long): Int = plan.entries.count { subjectId in it.subjectIds }

    /**
     * Deterministic signature of everything that defines the effective plan.
     * Any change (lesson order, subjects, item set, names, notes, categories, icons, labels) changes it.
     */
    fun signature(plan: GeneratedPlan): String = buildString {
        append(if (plan.isSchoolDay) "S" else "N").append('|')
        plan.lessons.forEach { append(it.position).append(':').append(it.subjectId).append(':').append(it.subjectName).append(':').append(it.iconKey).append(';') }
        append('|')
        plan.entries.sortedBy { it.key.toString() }.forEach { e ->
            append(e.key).append('=').append(e.name).append('/').append(e.note ?: "").append('/')
                .append(e.category.key).append('/').append(e.iconKey).append('/')
                .append(e.subjectLabels.joinToString(",")).append('/').append(e.forTheDay).append(';')
        }
    }.let { sha256(it) }

    private fun sha256(s: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
