package com.classprep.junior.data.repository

import com.classprep.junior.domain.AppClock
import com.classprep.junior.domain.model.DateTask
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.PlanState
import com.classprep.junior.domain.model.WeekConfig
import com.classprep.junior.domain.planning.ChecklistGenerator
import com.classprep.junior.domain.planning.ConfirmResult
import com.classprep.junior.domain.planning.ExampleData
import com.classprep.junior.domain.planning.Limits
import com.classprep.junior.domain.planning.PlanEngine
import com.classprep.junior.domain.planning.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * In-memory example week. Completely isolated from the database: nothing here is persisted,
 * scheduled for reminders, or written to history. State is discarded when the process ends.
 */
class ExamplePlanSource(private val clock: AppClock) : PlanSource {
    override val isExample = true

    private data class State(val plans: Map<LocalDate, PlanState> = emptyMap(), val extraTasks: Map<LocalDate, List<DateTask>> = emptyMap())

    private val state = MutableStateFlow(State())
    private var nextTaskId = -5000L

    private fun tasks(date: LocalDate, s: State) = ExampleData.tasksFor(date) + s.extraTasks[date].orEmpty()

    override fun observeConfig(): Flow<WeekConfig> = flowOf(ExampleData.config)

    override fun observePlan(date: LocalDate): Flow<PlanData> = state.map { s ->
        PlanData(ExampleData.config, ChecklistGenerator.generate(date, ExampleData.config, tasks(date, s)), s.plans[date])
    }

    private fun ensure(s: State, date: LocalDate): State {
        val gen = ChecklistGenerator.generate(date, ExampleData.config, tasks(date, s))
        val plan = s.plans[date] ?: PlanEngine.newPlan(date.toEpochDay(), gen)
        val reconciled = PlanEngine.reconcile(plan, gen, clock.instant()).plan
        return s.copy(plans = s.plans + (date to reconciled))
    }

    override suspend fun preparePlan(date: LocalDate) = state.update { ensure(it, date) }

    override suspend fun setReady(date: LocalDate, key: ItemKey, ready: Boolean) = state.update { s0 ->
        val s = ensure(s0, date)
        val plan = s.plans.getValue(date)
        s.copy(plans = s.plans + (date to PlanEngine.setReady(plan, key, ready, clock.instant())))
    }

    override suspend fun undoReady(date: LocalDate) = state.update { s0 ->
        val s = ensure(s0, date)
        s.copy(plans = s.plans + (date to PlanEngine.undoConfirmation(s.plans.getValue(date))))
    }

    override suspend fun confirm(date: LocalDate, expectedRevision: Int): ConfirmResult {
        var result: ConfirmResult = ConfirmResult.Rejected.STALE
        state.update { s0 ->
            val s = ensure(s0, date)
            val gen = ChecklistGenerator.generate(date, ExampleData.config, tasks(date, s))
            result = PlanEngine.confirm(s.plans.getValue(date), gen, expectedRevision, clock.instant())
            val r = result
            // The example never writes history; only the in-memory plan state changes.
            if (r is ConfirmResult.Confirmed) s.copy(plans = s.plans + (date to r.plan)) else s
        }
        return result
    }

    override suspend fun addTask(date: LocalDate, name: String, note: String?, category: ItemCategory, iconKey: String): String? {
        Validation.itemName(name)?.let { return it }
        note?.let { n -> Validation.note(n)?.let { return it } }
        if (state.value.extraTasks[date].orEmpty().size + 1 >= Limits.MAX_TASKS_PER_DATE) return "Task limit reached."
        val task = DateTask(nextTaskId--, date, name.trim(), note?.trim()?.ifEmpty { null }, iconKey, category)
        state.update { s -> ensure(s.copy(extraTasks = s.extraTasks + (date to s.extraTasks[date].orEmpty() + task)), date) }
        return null
    }
}
