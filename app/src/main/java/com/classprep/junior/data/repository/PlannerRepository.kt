package com.classprep.junior.data.repository

import androidx.room.withTransaction
import com.classprep.junior.data.local.AppDatabase
import com.classprep.junior.data.local.LessonSlotEntity
import com.classprep.junior.data.local.ReadinessHistoryEntity
import com.classprep.junior.data.local.ReadinessHistoryItemEntity
import com.classprep.junior.data.local.ReadinessHistoryLessonEntity
import com.classprep.junior.data.local.SubjectItemLinkEntity
import com.classprep.junior.data.local.WeekdayConfigEntity
import com.classprep.junior.data.local.WeekdayItemLinkEntity
import com.classprep.junior.domain.AppClock
import com.classprep.junior.domain.model.DateTask
import com.classprep.junior.domain.model.GeneratedPlan
import com.classprep.junior.domain.model.HistoryEntry
import com.classprep.junior.domain.model.HistoryItemSnapshot
import com.classprep.junior.domain.model.HistoryLessonSnapshot
import com.classprep.junior.domain.model.HistorySnapshot
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.PlanState
import com.classprep.junior.domain.model.PlanStatus
import com.classprep.junior.domain.model.ReusableItem
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.model.WeekConfig
import com.classprep.junior.domain.planning.ChecklistGenerator
import com.classprep.junior.domain.planning.ConfigEdit
import com.classprep.junior.domain.planning.ConfirmResult
import com.classprep.junior.domain.planning.Limits
import com.classprep.junior.domain.planning.PlanEngine
import com.classprep.junior.domain.planning.Validation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Single source of truth for timetable, items, plans and history.
 * All writes run off the main thread inside Room transactions; every configuration change reconciles
 * today's and future plans in the same transaction and then notifies [onConfigChanged] (reminder rescheduling).
 */
class PlannerRepository(
    private val db: AppDatabase,
    private val clock: AppClock,
    private val onConfigChanged: suspend () -> Unit,
) : PlanSource {
    private val config = db.configDao()
    private val plans = db.planDao()
    private val history = db.historyDao()

    override val isExample = false

    // ---------- Reading ----------

    override fun observeConfig(): Flow<WeekConfig> {
        val a = combine(config.observeSubjects(), config.observeWeekdays(), config.observeLessons()) { s, w, l -> Triple(s, w, l) }
        val b = combine(config.observeItems(), config.observeSubjectLinks(), config.observeWeekdayLinks()) { i, sl, wl -> Triple(i, sl, wl) }
        return combine(a, b) { (s, w, l), (i, sl, wl) -> buildConfig(s, w, l, i, sl, wl) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    suspend fun loadConfig(): WeekConfig = withContext(Dispatchers.IO) {
        buildConfig(config.subjects(), config.weekdays(), config.lessons(), config.items(), config.subjectLinks(), config.weekdayLinks())
    }

    fun observeTasks(date: LocalDate): Flow<List<DateTask>> =
        config.observeTasks(date.toString()).map { list -> list.map { it.toDomain() } }

    fun observeTasksFrom(date: LocalDate): Flow<List<DateTask>> =
        config.observeTasksFrom(date.toString()).map { list -> list.map { it.toDomain() } }

    override fun observePlan(date: LocalDate): Flow<PlanData> {
        val key = date.toString()
        val planFlow = combine(plans.observePlan(key), plans.observeStates(key)) { p, s -> p?.let { planToDomain(it, s) } }
        return combine(observeConfig(), observeTasks(date), planFlow) { cfg, tasks, plan ->
            PlanData(cfg, ChecklistGenerator.generate(date, cfg, tasks), plan)
        }.flowOn(Dispatchers.Default)
    }

    suspend fun generate(date: LocalDate): GeneratedPlan = withContext(Dispatchers.IO) {
        ChecklistGenerator.generate(date, loadConfig(), config.tasks(date.toString()).map { it.toDomain() })
    }

    suspend fun planStatus(date: LocalDate): PlanStatus = withContext(Dispatchers.IO) {
        val p = plans.plan(date.toString()) ?: return@withContext PlanStatus.OPEN
        PlanEngine.status(planToDomain(p, emptyList()))
    }

    suspend fun weekdaysUsingSubject(subjectId: Long): List<DayOfWeek> = withContext(Dispatchers.IO) {
        config.lessons().filter { it.subjectId == subjectId }.map { DayOfWeek.of(it.weekday) }.distinct().sorted()
    }

    // ---------- Plans ----------

    private suspend fun loadPlan(date: LocalDate): PlanState? {
        val p = plans.plan(date.toString()) ?: return null
        return planToDomain(p, plans.states(p.id))
    }

    private suspend fun savePlan(plan: PlanState) {
        plans.upsertPlan(plan.toEntity())
        plans.deleteStates(plan.id)
        plans.insertStates(plan.stateEntities())
    }

    /** Creates or reconciles the plan for [date]. Must be called inside a transaction. */
    private suspend fun ensurePlanLocked(date: LocalDate, cfg: WeekConfig): Pair<PlanState, GeneratedPlan> {
        val generated = ChecklistGenerator.generate(date, cfg, config.tasks(date.toString()).map { it.toDomain() })
        val now = clock.instant()
        val existing = loadPlan(date)
        if (existing == null) {
            val skeleton = PlanEngine.newPlan(0, generated)
            val id = plans.insertPlan(skeleton.toEntity())
            val created = PlanEngine.reconcile(skeleton.copy(id = id), generated, now).plan
            savePlan(created)
            return created to generated
        }
        val result = PlanEngine.reconcile(existing, generated, now)
        if (result.changed) savePlan(result.plan)
        return result.plan to generated
    }

    override suspend fun preparePlan(date: LocalDate) {
        withContext(Dispatchers.IO) {
            db.withTransaction { ensurePlanLocked(date, loadConfig()) }
        }
    }

    override suspend fun setReady(date: LocalDate, key: ItemKey, ready: Boolean) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val (plan, generated) = ensurePlanLocked(date, loadConfig())
                if (generated.entries.none { it.key == key }) return@withTransaction
                val updated = PlanEngine.setReady(plan, key, ready, clock.instant())
                if (updated != plan) savePlan(updated)
            }
        }
    }

    override suspend fun undoReady(date: LocalDate) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val (plan, _) = ensurePlanLocked(date, loadConfig())
                val updated = PlanEngine.undoConfirmation(plan)
                if (updated != plan) savePlan(updated)
            }
        }
    }

    /** Atomic: validates, marks confirmed and writes the immutable history snapshot in one transaction. */
    override suspend fun confirm(date: LocalDate, expectedRevision: Int): ConfirmResult = withContext(Dispatchers.IO) {
        db.withTransaction {
            val (plan, generated) = ensurePlanLocked(date, loadConfig())
            val result = PlanEngine.confirm(plan, generated, expectedRevision, clock.instant())
            if (result is ConfirmResult.Confirmed) {
                plans.upsertPlan(result.plan.toEntity())
                insertHistory(result.snapshot)
                history.trimTo(Limits.HISTORY_KEEP)
            }
            result
        }
    }

    private suspend fun insertHistory(s: HistorySnapshot) {
        val id = history.insertEntry(
            ReadinessHistoryEntity(
                targetDate = s.targetDate.toString(),
                confirmedAt = s.confirmedAt.toEpochMilli(),
                itemCount = s.itemCount,
                planRevision = s.planRevision,
            ),
        )
        history.insertLessons(s.lessons.map { ReadinessHistoryLessonEntity(id, it.position, it.subjectName, it.iconKey) })
        history.insertItems(
            s.items.mapIndexed { i, it ->
                ReadinessHistoryItemEntity(
                    historyId = id, sortOrder = i, nameSnapshot = it.name, noteSnapshot = it.note,
                    categorySnapshot = it.category.key, iconKeySnapshot = it.iconKey, subjectLabelsSnapshot = it.subjectLabels,
                )
            },
        )
    }

    /** Reconciles all existing plans from today on. Past plans are left untouched. Call inside a transaction. */
    private suspend fun reconcileActivePlansLocked() {
        val today = clock.today()
        plans.deletePlansBefore(today.minusDays(30).toString())
        val cfg = loadConfig()
        plans.plansFrom(today.toString()).forEach { ensurePlanLocked(LocalDate.parse(it.targetDate), cfg) }
    }

    /** Number of confirmed plans (today or later) whose effective content would change with [edit]. */
    suspend fun confirmedPlansAffectedBy(edit: ConfigEdit): Int = withContext(Dispatchers.IO) {
        val today = clock.today()
        val newCfg = edit.applyTo(loadConfig())
        plans.plansFrom(today.toString()).count { entity ->
            val plan = planToDomain(entity, emptyList())
            if (PlanEngine.status(plan) != PlanStatus.CONFIRMED) return@count false
            val date = LocalDate.parse(entity.targetDate)
            val gen = ChecklistGenerator.generate(date, newCfg, config.tasks(entity.targetDate).map { it.toDomain() })
            ChecklistGenerator.signature(gen) != plan.signature
        }
    }

    suspend fun isConfirmed(date: LocalDate): Boolean = planStatus(date) == PlanStatus.CONFIRMED

    // ---------- Configuration edits ----------

    private suspend fun <T> edit(block: suspend () -> T): T {
        val result = withContext(Dispatchers.IO) {
            db.withTransaction {
                val r = block()
                reconcileActivePlansLocked()
                r
            }
        }
        onConfigChanged()
        return result
    }

    /** Inserts when id == 0. Returns the subject id. */
    suspend fun saveSubject(subject: Subject): Long = edit {
        if (subject.id == 0L) {
            check(config.subjectCount() < Limits.MAX_SUBJECTS) { "You can add up to ${Limits.MAX_SUBJECTS} subjects." }
            config.insertSubject(subject.toEntity().copy(displayOrder = config.nextSubjectOrder()))
        } else {
            config.upsertSubject(subject.toEntity())
            subject.id
        }
    }

    suspend fun deleteSubject(subjectId: Long) = edit {
        config.deleteSubject(subjectId) // cascades lesson slots and subject links
        renumberAllLessons()
    }

    private suspend fun renumberAllLessons() {
        val byDay = config.lessons().groupBy { it.weekday }
        byDay.forEach { (day, slots) ->
            config.deleteLessonsFor(day)
            config.insertLessons(slots.sortedBy { it.position }.mapIndexed { i, s -> s.copy(id = 0, position = i + 1) })
        }
    }

    suspend fun saveWeekday(day: DayOfWeek, isSchoolDay: Boolean, subjectIds: List<Long>) = edit {
        require(subjectIds.size <= Limits.MAX_LESSONS_PER_DAY)
        config.upsertWeekday(WeekdayConfigEntity(day.value, isSchoolDay))
        config.deleteLessonsFor(day.value)
        if (isSchoolDay) {
            config.insertLessons(subjectIds.mapIndexed { i, s -> LessonSlotEntity(weekday = day.value, subjectId = s, position = i + 1) })
        } else {
            config.deleteWeekdayLinks(day.value)
        }
    }

    suspend fun saveSchoolDays(days: Set<DayOfWeek>) = edit {
        DayOfWeek.entries.forEach { d ->
            val school = d in days
            config.upsertWeekday(WeekdayConfigEntity(d.value, school))
            if (!school) {
                config.deleteLessonsFor(d.value)
                config.deleteWeekdayLinks(d.value)
            }
        }
    }

    suspend fun saveItem(item: ReusableItem): Long = edit {
        if (item.id == 0L) {
            check(config.itemCount() < Limits.MAX_ITEMS) { "You can add up to ${Limits.MAX_ITEMS} items." }
            config.insertItem(item.toEntity())
        } else {
            config.upsertItem(item.toEntity())
            item.id
        }
    }

    suspend fun deleteItem(itemId: Long) = edit { config.deleteItem(itemId) }

    suspend fun setSubjectItems(subjectId: Long, itemIds: Set<Long>) = edit {
        config.deleteSubjectLinks(subjectId)
        config.insertSubjectLinks(itemIds.map { SubjectItemLinkEntity(subjectId, it) })
    }

    suspend fun setWeekdayItems(day: DayOfWeek, itemIds: Set<Long>) = edit {
        config.deleteWeekdayLinks(day.value)
        config.insertWeekdayLinks(itemIds.map { WeekdayItemLinkEntity(day.value, it) })
    }

    // ---------- Date-specific tasks ----------

    override suspend fun addTask(date: LocalDate, name: String, note: String?, category: ItemCategory, iconKey: String): String? =
        saveTask(DateTask(0, date, name, note, iconKey, category))

    /** Returns an error message or null. */
    suspend fun saveTask(task: DateTask): String? {
        Validation.itemName(task.name)?.let { return it }
        task.note?.let { n -> Validation.note(n)?.let { return it } }
        val result = withContext(Dispatchers.IO) {
            db.withTransaction {
                val dateKey = task.targetDate.toString()
                if (task.id == 0L && config.taskCount(dateKey) >= Limits.MAX_TASKS_PER_DATE) {
                    return@withTransaction "You can add up to ${Limits.MAX_TASKS_PER_DATE} tasks for one day."
                }
                val entity = task.copy(name = task.name.trim(), note = task.note?.trim()?.takeIf { it.isNotEmpty() }).toEntity()
                if (task.id == 0L) config.insertTask(entity) else config.upsertTask(entity)
                // A task edit moving the task to another date must reconcile both dates.
                ensurePlanLocked(task.targetDate, loadConfig())
                reconcileActivePlansLocked()
                null
            }
        }
        if (result == null) onConfigChanged()
        return result
    }

    suspend fun deleteTask(id: Long) = edit { config.deleteTask(id) }

    suspend fun task(id: Long): DateTask? = withContext(Dispatchers.IO) { config.task(id)?.toDomain() }

    // ---------- History ----------

    fun observeHistory(): Flow<List<HistoryEntry>> = history.observeHistory().map { list ->
        list.map { HistoryEntry(it.id, LocalDate.parse(it.targetDate), Instant.ofEpochMilli(it.confirmedAt), it.itemCount, it.planRevision) }
    }

    suspend fun historyDetail(id: Long): HistorySnapshot? = withContext(Dispatchers.IO) {
        val e = history.entry(id) ?: return@withContext null
        HistorySnapshot(
            targetDate = LocalDate.parse(e.targetDate),
            confirmedAt = Instant.ofEpochMilli(e.confirmedAt),
            planRevision = e.planRevision,
            lessons = history.lessons(id).map { HistoryLessonSnapshot(it.position, it.subjectNameSnapshot, it.iconKeySnapshot) },
            items = history.items(id).map {
                HistoryItemSnapshot(it.nameSnapshot, it.noteSnapshot, ItemCategory.fromKey(it.categorySnapshot), it.iconKeySnapshot, it.subjectLabelsSnapshot)
            },
        )
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) { history.delete(id) }

    suspend fun clearHistory() = withContext(Dispatchers.IO) { history.clear() }

    // ---------- Reset ----------

    suspend fun clearAllTables() = withContext(Dispatchers.IO) { db.clearAllTables() }
}
