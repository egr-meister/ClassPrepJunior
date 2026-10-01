package com.classprep.junior.ui.planner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classprep.junior.data.repository.PlanData
import com.classprep.junior.data.repository.PlanSource
import com.classprep.junior.domain.AppClock
import com.classprep.junior.domain.model.ChecklistEntry
import com.classprep.junior.domain.model.IconKeys
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.PlanStatus
import com.classprep.junior.domain.planning.ChecklistGenerator
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.domain.planning.PlanEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayCell(
    val date: LocalDate,
    val shortName: String,
    val dayOfMonth: Int,
    val isSchoolDay: Boolean,
    val selectable: Boolean,
    val selected: Boolean,
    val isToday: Boolean,
)

data class LessonRow(val position: Int, val subjectId: Long, val name: String, val iconKey: String, val itemCount: Int)

data class ChecklistRow(
    val key: ItemKey,
    val name: String,
    val note: String?,
    val iconKey: String,
    val relatedLabel: String,
    val ready: Boolean,
)

data class ChecklistSection(val title: String, val rows: List<ChecklistRow>)

data class PlannerUiState(
    val loading: Boolean = true,
    val isExample: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
    val date: LocalDate = LocalDate.MIN,
    val header: String = "",
    val longDate: String = "",
    val isTomorrow: Boolean = false,
    val isSchoolDay: Boolean = false,
    val editable: Boolean = false,
    val week: List<DayCell> = emptyList(),
    val canPrevWeek: Boolean = false,
    val canNextWeek: Boolean = false,
    val lessons: List<LessonRow> = emptyList(),
    val filter: LessonRow? = null,
    val sections: List<ChecklistSection> = emptyList(),
    val ready: Int = 0,
    val total: Int = 0,
    val status: PlanStatus = PlanStatus.OPEN,
    val overLimit: Boolean = false,
    val nextSchoolDay: LocalDate? = null,
    val hasAnySchoolDay: Boolean = false,
    val taskCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(
    private val source: PlanSource,
    private val clock: AppClock,
    timeVersion: Flow<Long>,
    private val savedState: SavedStateHandle,
    initialDate: LocalDate?,
) : ViewModel() {

    private val todayFlow: StateFlow<LocalDate> = timeVersion.map { clock.today() }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, clock.today())

    private val selected = MutableStateFlow(
        savedState.get<String>(KEY_DATE)?.let(LocalDate::parse)
            ?: initialDate?.let { DateSelection.clampToEditable(it, clock.today()) }
            ?: DateSelection.defaultDate(clock.today()),
    )
    private val weekStart = MutableStateFlow(DateSelection.weekStart(selected.value))
    private val filterSubject = MutableStateFlow<Long?>(null)

    val state: StateFlow<PlannerUiState> = combine(
        todayFlow,
        selected,
        weekStart,
        filterSubject,
        selected.flatMapLatest { source.observePlan(it) },
    ) { today, date, ws, filter, data -> buildState(today, date, ws, filter, data) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannerUiState())

    init {
        viewModelScope.launch {
            selected.collect { d ->
                savedState[KEY_DATE] = d.toString()
                if (DateSelection.isEditable(d, clock.today())) source.preparePlan(d)
            }
        }
        // Date rollover or time-zone change: never keep a date that has become the past.
        viewModelScope.launch {
            todayFlow.collect { today ->
                if (selected.value.isBefore(today)) selectDate(DateSelection.defaultDate(today))
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val today = todayFlow.value
        if (!DateSelection.isEditable(date, today)) return
        selected.value = date
        weekStart.value = DateSelection.weekStart(date)
        filterSubject.value = null
    }

    /** Opens the plan for [date] (e.g. from a notification). */
    fun openDate(date: LocalDate) = selectDate(DateSelection.clampToEditable(date, todayFlow.value))

    fun previousWeek() {
        val ws = weekStart.value
        if (DateSelection.canGoToPreviousWeek(ws, todayFlow.value)) weekStart.value = ws.minusWeeks(1)
    }

    fun nextWeek() {
        val ws = weekStart.value
        if (DateSelection.canGoToNextWeek(ws, todayFlow.value)) weekStart.value = ws.plusWeeks(1)
    }

    fun setFilter(subjectId: Long?) {
        filterSubject.value = subjectId
    }

    fun toggle(key: ItemKey, ready: Boolean) {
        val d = selected.value
        if (!DateSelection.isEditable(d, todayFlow.value)) return
        viewModelScope.launch { source.setReady(d, key, ready) }
    }

    fun undoReady() {
        val d = selected.value
        viewModelScope.launch { source.undoReady(d) }
    }

    private val _taskError = MutableStateFlow<String?>(null)
    val taskError: StateFlow<String?> = _taskError

    fun addTask(name: String, note: String, onDone: () -> Unit) {
        val d = selected.value
        viewModelScope.launch {
            val err = source.addTask(d, name, note.ifBlank { null }, ItemCategory.HOMEWORK, IconKeys.ITEM_TASK)
            _taskError.value = err
            if (err == null) onDone()
        }
    }

    fun clearTaskError() {
        _taskError.value = null
    }

    private fun buildState(today: LocalDate, date: LocalDate, ws: LocalDate, filter: Long?, data: PlanData): PlannerUiState {
        val gen = data.generated
        val plan = data.plan
        val lessons = gen.lessons.map {
            LessonRow(it.position, it.subjectId, it.subjectName, it.iconKey, ChecklistGenerator.countForSubject(gen, it.subjectId))
        }
        val filterRow = filter?.let { f -> lessons.firstOrNull { it.subjectId == f } }
        val progress = PlanEngine.progress(plan, gen)
        return PlannerUiState(
            loading = false,
            isExample = source.isExample,
            today = today,
            date = date,
            header = DateSelection.headerContext(date, today, gen.isSchoolDay),
            longDate = DateSelection.longDate(date),
            isTomorrow = DateSelection.isTomorrow(date, today),
            isSchoolDay = gen.isSchoolDay,
            editable = DateSelection.isEditable(date, today),
            week = DateSelection.weekDays(ws).map { d ->
                DayCell(
                    date = d,
                    shortName = DateSelection.shortDayName(d.dayOfWeek),
                    dayOfMonth = d.dayOfMonth,
                    isSchoolDay = data.config.isSchoolDay(d.dayOfWeek),
                    selectable = DateSelection.isEditable(d, today),
                    selected = d == date,
                    isToday = d == today,
                )
            },
            canPrevWeek = DateSelection.canGoToPreviousWeek(ws, today),
            canNextWeek = DateSelection.canGoToNextWeek(ws, today),
            lessons = lessons,
            filter = filterRow,
            sections = sections(gen.entries, { k -> PlanEngine.isReady(plan, k) }, filterRow?.subjectId),
            ready = progress.ready,
            total = progress.total,
            status = PlanEngine.status(plan),
            overLimit = gen.overLimit,
            nextSchoolDay = DateSelection.nextSchoolDay(date, today, data.config.schoolDays),
            hasAnySchoolDay = data.config.schoolDays.isNotEmpty(),
            taskCount = gen.entries.count { it.isTask },
        )
    }

    companion object {
        private const val KEY_DATE = "planner_date"

        fun row(e: ChecklistEntry, ready: Boolean) = ChecklistRow(
            key = e.key,
            name = e.name,
            note = e.note,
            iconKey = e.iconKey,
            relatedLabel = PlanEngine.labelFor(e.subjectLabels, e.forTheDay, e.isTask),
            ready = ready,
        )

        /** Category sections, then a separate "For the day" section for general weekday items. */
        fun sections(entries: List<ChecklistEntry>, isReady: (ItemKey) -> Boolean, filterSubjectId: Long?): List<ChecklistSection> {
            val visible = if (filterSubjectId != null) entries.filter { filterSubjectId in it.subjectIds } else entries
            val general = if (filterSubjectId == null) visible.filter { it.forTheDay && it.subjectIds.isEmpty() } else emptyList()
            val main = visible - general.toSet()
            val result = ItemCategory.entries.mapNotNull { cat ->
                val rows = main.filter { it.category == cat }.map { row(it, isReady(it.key)) }
                if (rows.isEmpty()) null else ChecklistSection(cat.label, rows)
            }.toMutableList()
            if (general.isNotEmpty()) result += ChecklistSection("For the day", general.map { row(it, isReady(it.key)) })
            return result
        }
    }
}
