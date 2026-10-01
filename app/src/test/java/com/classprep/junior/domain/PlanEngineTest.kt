package com.classprep.junior.domain

import com.classprep.junior.domain.model.GeneratedPlan
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.PlanState
import com.classprep.junior.domain.model.PlanStatus
import com.classprep.junior.domain.model.SourceType
import com.classprep.junior.domain.planning.ChecklistGenerator
import com.classprep.junior.domain.planning.ConfigEdit
import com.classprep.junior.domain.planning.ConfirmResult
import com.classprep.junior.domain.planning.PlanEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant

class PlanEngineTest {
    private val now = Instant.parse("2026-10-04T16:00:00Z")
    private val cfg = Fx.config()
    private val date = Fx.MONDAY_DATE

    private fun fresh(gen: GeneratedPlan = ChecklistGenerator.generate(date, cfg, emptyList())): Pair<PlanState, GeneratedPlan> {
        val plan = PlanEngine.reconcile(PlanEngine.newPlan(1, gen), gen, now).plan
        return plan to gen
    }

    private fun checkAll(plan: PlanState, gen: GeneratedPlan) =
        gen.entries.fold(plan) { p, e -> PlanEngine.setReady(p, e.key, true, now) }

    @Test fun readinessCountsReadyOverTotal() {
        val (plan, gen) = fresh()
        val p1 = PlanEngine.setReady(plan, gen.entries.first().key, true, now)
        val progress = PlanEngine.progress(p1, gen)
        assertEquals(1, progress.ready)
        assertEquals(gen.entries.size, progress.total)
        assertFalse(progress.complete)
    }

    @Test fun cannotConfirmIncompleteOrEmpty() {
        val (plan, gen) = fresh()
        assertEquals(ConfirmResult.Rejected.INCOMPLETE, PlanEngine.confirm(plan, gen, plan.revision, now))
        val saturday = date.plusDays(5)
        val emptyGen = ChecklistGenerator.generate(saturday, cfg, emptyList())
        val (emptyPlan, _) = fresh(emptyGen)
        assertEquals(ConfirmResult.Rejected.EMPTY, PlanEngine.confirm(emptyPlan, emptyGen, emptyPlan.revision, now))
    }

    @Test fun confirmAndPreventDuplicate() {
        val (plan, gen) = fresh()
        val all = checkAll(plan, gen)
        val r = PlanEngine.confirm(all, gen, all.revision, now)
        assertTrue(r is ConfirmResult.Confirmed)
        val confirmed = (r as ConfirmResult.Confirmed).plan
        assertEquals(PlanStatus.CONFIRMED, PlanEngine.status(confirmed))
        assertEquals(gen.entries.size, r.snapshot.itemCount)
        // Second tap on an unchanged confirmed plan is rejected (no duplicate history).
        assertEquals(ConfirmResult.Rejected.ALREADY_CONFIRMED, PlanEngine.confirm(confirmed, gen, confirmed.revision, now))
    }

    @Test fun staleRevisionRejected() {
        val (plan, gen) = fresh()
        val all = checkAll(plan, gen)
        assertEquals(ConfirmResult.Rejected.STALE, PlanEngine.confirm(all, gen, all.revision - 1, now))
    }

    @Test fun uncheckingInvalidatesConfirmation() {
        val (plan, gen) = fresh()
        val all = checkAll(plan, gen)
        val confirmed = (PlanEngine.confirm(all, gen, all.revision, now) as ConfirmResult.Confirmed).plan
        val unchecked = PlanEngine.setReady(confirmed, gen.entries.first().key, false, now)
        assertEquals(PlanStatus.NEEDS_REVIEW, PlanEngine.status(unchecked))
        // Re-check everything → can confirm again as a new revision.
        val rechecked = PlanEngine.setReady(unchecked, gen.entries.first().key, true, now)
        assertEquals(PlanStatus.NEEDS_REVIEW, PlanEngine.status(rechecked))
        assertTrue(PlanEngine.confirm(rechecked, gen, rechecked.revision, now) is ConfirmResult.Confirmed)
    }

    @Test fun undoAllowsReconfirmation() {
        val (plan, gen) = fresh()
        val all = checkAll(plan, gen)
        val confirmed = (PlanEngine.confirm(all, gen, all.revision, now) as ConfirmResult.Confirmed).plan
        val undone = PlanEngine.undoConfirmation(confirmed)
        assertEquals(PlanStatus.NEEDS_REVIEW, PlanEngine.status(undone))
        val again = PlanEngine.confirm(undone, gen, undone.revision, now)
        assertTrue(again is ConfirmResult.Confirmed)
        assertEquals(undone.revision, (again as ConfirmResult.Confirmed).snapshot.planRevision)
    }

    @Test fun reconciliationKeepsUnchangedAddsNewDropsRemoved() {
        val (plan, gen) = fresh()
        val all = checkAll(plan, gen)
        val confirmed = (PlanEngine.confirm(all, gen, all.revision, now) as ConfirmResult.Confirmed).plan

        // Parent removes the ruler from Maths and adds sports clothes to Monday.
        val edited = ConfigEdit.SetWeekdayItems(DayOfWeek.MONDAY, setOf(Fx.SPORTS)).applyTo(
            ConfigEdit.SetSubjectItems(Fx.MATH, setOf(Fx.MATH_NOTEBOOK, Fx.PENCIL_CASE)).applyTo(cfg),
        )
        val tasks = listOf(Fx.task(500, date))
        val newGen = ChecklistGenerator.generate(date, edited, tasks)
        val result = PlanEngine.reconcile(confirmed, newGen, now)
        assertTrue(result.changed)
        val p = result.plan
        val ruler = ItemKey(SourceType.ITEM, Fx.RULER)
        val notebook = ItemKey(SourceType.ITEM, Fx.MATH_NOTEBOOK)
        val sports = ItemKey(SourceType.ITEM, Fx.SPORTS)
        val task = ItemKey(SourceType.TASK, 500)
        assertFalse(ruler in p.states)
        assertTrue(p.states.getValue(notebook).ready)
        assertFalse(p.states.getValue(sports).ready)
        assertFalse(p.states.getValue(task).ready)
        assertEquals(PlanStatus.NEEDS_REVIEW, PlanEngine.status(p))
    }

    @Test fun reconciliationWithoutChangeKeepsConfirmation() {
        val (plan, gen) = fresh()
        val all = checkAll(plan, gen)
        val confirmed = (PlanEngine.confirm(all, gen, all.revision, now) as ConfirmResult.Confirmed).plan
        val again = PlanEngine.reconcile(confirmed, ChecklistGenerator.generate(date, cfg, emptyList()), now)
        assertFalse(again.changed)
        assertEquals(PlanStatus.CONFIRMED, PlanEngine.status(again.plan))
    }

    @Test fun historySnapshotIsImmutableAfterEdits() {
        val (plan, gen) = fresh()
        val all = checkAll(plan, gen)
        val snapshot = (PlanEngine.confirm(all, gen, all.revision, now) as ConfirmResult.Confirmed).snapshot
        val before = snapshot.copy()
        // Rename subject and delete an item afterwards.
        val edited = ConfigEdit.DeleteItem(Fx.PENCIL_CASE).applyTo(
            ConfigEdit.UpsertSubject(cfg.subject(Fx.MATH)!!.copy(name = "Maths")).applyTo(cfg),
        )
        ChecklistGenerator.generate(date, edited, emptyList())
        assertEquals(before, snapshot)
        assertEquals("Mathematics", snapshot.lessons.first().subjectName)
        assertTrue(snapshot.items.any { it.name == "Pencil case" })
    }

    @Test fun deletingSubjectRenumbersLessons() {
        val edited = ConfigEdit.DeleteSubject(Fx.PE).applyTo(cfg)
        val tue = edited.lessonsFor(DayOfWeek.TUESDAY)
        assertEquals(listOf(1, 2, 3), tue.map { it.position })
        assertTrue(tue.none { it.subjectId == Fx.PE })
    }

    @Test fun nonSchoolDayEditRemovesLessonsAndWeekdayItems() {
        val edited = ConfigEdit.SetWeekday(DayOfWeek.TUESDAY, false, emptyList()).applyTo(cfg)
        assertFalse(edited.isSchoolDay(DayOfWeek.TUESDAY))
        assertTrue(edited.lessonsFor(DayOfWeek.TUESDAY).isEmpty())
        assertTrue(edited.weekdayItems[DayOfWeek.TUESDAY].isNullOrEmpty())
    }
}
