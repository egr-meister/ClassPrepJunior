package com.classprep.junior.domain

import com.classprep.junior.domain.Fx.ENGLISH_NOTEBOOK
import com.classprep.junior.domain.Fx.MATH_NOTEBOOK
import com.classprep.junior.domain.Fx.PENCIL_CASE
import com.classprep.junior.domain.Fx.SPORTS
import com.classprep.junior.domain.Fx.UNIFORM
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.SourceType
import com.classprep.junior.domain.planning.ChecklistGenerator
import com.classprep.junior.domain.planning.Limits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistGeneratorTest {
    private val cfg = Fx.config()

    @Test fun repeatedLessonsDoNotDuplicateItems() {
        val plan = ChecklistGenerator.generate(Fx.TUESDAY_DATE, cfg, emptyList())
        assertEquals(listOf(1, 2, 3, 4), plan.lessons.map { it.position })
        val keys = plan.entries.map { it.key }
        assertEquals(keys.toSet().size, keys.size)
        // Maths twice + English: pencil case appears once with both subject labels.
        val pencil = plan.entries.single { it.key == ItemKey(SourceType.ITEM, PENCIL_CASE) }
        assertEquals(listOf("Mathematics", "English"), pencil.subjectLabels)
    }

    @Test fun sameNameItemsAreNotMerged() {
        val plan = ChecklistGenerator.generate(Fx.MONDAY_DATE, cfg, emptyList())
        val notebooks = plan.entries.filter { it.name == "Notebook" }
        assertEquals(setOf(MATH_NOTEBOOK, ENGLISH_NOTEBOOK), notebooks.map { it.key.sourceId }.toSet())
    }

    @Test fun weekdayItemsAreForTheDayAndSharedWithSubjects() {
        val plan = ChecklistGenerator.generate(Fx.TUESDAY_DATE, cfg, emptyList())
        val uniform = plan.entries.single { it.key.sourceId == UNIFORM }
        assertTrue(uniform.forTheDay)
        assertTrue(uniform.subjectLabels.isEmpty())
        val sports = plan.entries.single { it.key.sourceId == SPORTS } // PE item AND Tuesday item
        assertTrue(sports.forTheDay)
        assertEquals(listOf("Physical Education"), sports.subjectLabels)
    }

    @Test fun dateTasksIncludedOnlyOnTheirDate() {
        val tasks = listOf(Fx.task(100, Fx.MONDAY_DATE), Fx.task(101, Fx.TUESDAY_DATE, "Worksheet"))
        val plan = ChecklistGenerator.generate(Fx.MONDAY_DATE, cfg, tasks)
        val taskEntries = plan.entries.filter { it.key.sourceType == SourceType.TASK }
        assertEquals(listOf(100L), taskEntries.map { it.key.sourceId })
    }

    @Test fun nonSchoolDayHasNoLessonsOrRecurringItems() {
        val saturday = Fx.MONDAY_DATE.plusDays(5)
        val plan = ChecklistGenerator.generate(saturday, cfg, emptyList())
        assertFalse(plan.isSchoolDay)
        assertTrue(plan.lessons.isEmpty())
        assertTrue(plan.isEmpty)
    }

    @Test fun overLimitIsReportedNeverTruncated() {
        val many = (1..Limits.MAX_CHECKLIST_ENTRIES + 5).map { Fx.task(1000L + it, Fx.MONDAY_DATE, "Task $it") }
        val plan = ChecklistGenerator.generate(Fx.MONDAY_DATE, cfg, many)
        assertTrue(plan.overLimit)
        assertEquals(Limits.MAX_CHECKLIST_ENTRIES + 5, plan.entries.count { it.isTask })
    }

    @Test fun signatureChangesWithContent() {
        val a = ChecklistGenerator.signature(ChecklistGenerator.generate(Fx.MONDAY_DATE, cfg, emptyList()))
        val b = ChecklistGenerator.signature(ChecklistGenerator.generate(Fx.MONDAY_DATE, cfg, emptyList()))
        assertEquals(a, b)
        val renamed = cfg.copy(items = cfg.items.map { if (it.id == PENCIL_CASE) it.copy(name = "Pencils") else it })
        val c = ChecklistGenerator.signature(ChecklistGenerator.generate(Fx.MONDAY_DATE, renamed, emptyList()))
        assertNotEquals(a, c)
    }
}
