package com.classprep.junior.domain.planning

import com.classprep.junior.domain.model.GeneratedPlan
import com.classprep.junior.domain.model.HistoryItemSnapshot
import com.classprep.junior.domain.model.HistoryLessonSnapshot
import com.classprep.junior.domain.model.HistorySnapshot
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.ItemState
import com.classprep.junior.domain.model.PlanState
import com.classprep.junior.domain.model.PlanStatus
import java.time.Instant

data class Progress(val ready: Int, val total: Int) {
    val complete get() = total > 0 && ready == total
}

sealed interface ConfirmResult {
    data class Confirmed(val plan: PlanState, val snapshot: HistorySnapshot) : ConfirmResult
    enum class Rejected : ConfirmResult { EMPTY, INCOMPLETE, ALREADY_CONFIRMED, STALE, OVER_LIMIT }
}

data class ReconcileResult(val plan: PlanState, val changed: Boolean)

/**
 * Pure state machine for preparation plans. The repository persists what these functions return,
 * inside a database transaction.
 *
 * Revision rules:
 * - The plan is confirmed only while confirmedRevision == revision.
 * - Any change to the effective checklist/timetable (signature) bumps the revision → "Needs review".
 * - Unchecking an item of a confirmed plan bumps the revision → "Needs review".
 * - Undoing readiness bumps the revision, so a new confirmation is a distinct history entry.
 */
object PlanEngine {

    fun newPlan(id: Long, generated: GeneratedPlan): PlanState = PlanState(
        id = id,
        date = generated.date,
        revision = 1,
        signature = ChecklistGenerator.signature(generated),
        confirmedRevision = null,
        confirmedAt = null,
        states = emptyMap(),
    )

    fun status(plan: PlanState?): PlanStatus = when {
        plan?.confirmedRevision == null -> PlanStatus.OPEN
        plan.confirmedRevision == plan.revision -> PlanStatus.CONFIRMED
        else -> PlanStatus.NEEDS_REVIEW
    }

    fun isReady(plan: PlanState?, key: ItemKey): Boolean = plan?.states?.get(key)?.ready == true

    fun progress(plan: PlanState?, generated: GeneratedPlan): Progress =
        Progress(generated.entries.count { isReady(plan, it.key) }, generated.entries.size)

    /**
     * Reconciles stored states with a freshly generated checklist:
     * keeps states for unchanged ids, adds new items unchecked, drops items no longer required,
     * and invalidates readiness when the effective plan changed.
     */
    fun reconcile(plan: PlanState, generated: GeneratedPlan, now: Instant): ReconcileResult {
        val keys = generated.entries.map { it.key }.toSet()
        val newStates = LinkedHashMap<ItemKey, ItemState>()
        for (key in keys) newStates[key] = plan.states[key] ?: ItemState(false, now)
        val newSignature = ChecklistGenerator.signature(generated)
        val signatureChanged = newSignature != plan.signature
        val statesChanged = newStates.keys != plan.states.keys
        if (!signatureChanged && !statesChanged) return ReconcileResult(plan, false)
        val updated = plan.copy(
            revision = if (signatureChanged) plan.revision + 1 else plan.revision,
            signature = newSignature,
            states = newStates,
        )
        return ReconcileResult(updated, true)
    }

    fun setReady(plan: PlanState, key: ItemKey, ready: Boolean, now: Instant): PlanState {
        val current = plan.states[key]
        if (current?.ready == ready) return plan
        val wasConfirmed = status(plan) == PlanStatus.CONFIRMED
        val states = plan.states + (key to ItemState(ready, now))
        return plan.copy(
            states = states,
            revision = if (!ready && wasConfirmed) plan.revision + 1 else plan.revision,
        )
    }

    fun undoConfirmation(plan: PlanState): PlanState =
        if (status(plan) == PlanStatus.CONFIRMED) plan.copy(revision = plan.revision + 1) else plan

    /**
     * Confirms readiness. [expectedRevision] is the revision the user reviewed; a mismatch means the plan
     * changed underneath the review screen and the confirmation is rejected (also guards duplicate taps).
     */
    fun confirm(plan: PlanState, generated: GeneratedPlan, expectedRevision: Int, now: Instant): ConfirmResult {
        if (generated.isEmpty) return ConfirmResult.Rejected.EMPTY
        if (generated.overLimit) return ConfirmResult.Rejected.OVER_LIMIT
        if (status(plan) == PlanStatus.CONFIRMED) return ConfirmResult.Rejected.ALREADY_CONFIRMED
        if (plan.revision != expectedRevision || plan.signature != ChecklistGenerator.signature(generated)) {
            return ConfirmResult.Rejected.STALE
        }
        if (!progress(plan, generated).complete) return ConfirmResult.Rejected.INCOMPLETE
        val confirmed = plan.copy(confirmedRevision = plan.revision, confirmedAt = now)
        return ConfirmResult.Confirmed(confirmed, snapshot(generated, plan.revision, now))
    }

    /** Immutable value copy of the plan at confirmation time. */
    fun snapshot(generated: GeneratedPlan, revision: Int, now: Instant): HistorySnapshot = HistorySnapshot(
        targetDate = generated.date,
        confirmedAt = now,
        planRevision = revision,
        lessons = generated.lessons.map { HistoryLessonSnapshot(it.position, it.subjectName, it.iconKey) },
        items = generated.entries.map {
            HistoryItemSnapshot(
                name = it.name,
                note = it.note,
                category = it.category,
                iconKey = it.iconKey,
                subjectLabels = labelFor(it.subjectLabels, it.forTheDay, it.isTask),
            )
        },
    )

    fun labelFor(subjectLabels: List<String>, forTheDay: Boolean, isTask: Boolean = false): String = when {
        subjectLabels.isNotEmpty() && forTheDay -> subjectLabels.joinToString(" · ") + " · For the day"
        subjectLabels.isNotEmpty() -> subjectLabels.joinToString(" · ")
        isTask -> "Homework or task"
        else -> "For the day"
    }
}
