package com.classprep.junior.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classprep.junior.data.repository.PlanSource
import com.classprep.junior.domain.AppClock
import com.classprep.junior.domain.model.PlanStatus
import com.classprep.junior.domain.planning.ConfirmResult
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.domain.planning.PlanEngine
import com.classprep.junior.ui.planner.ChecklistRow
import com.classprep.junior.ui.planner.ChecklistSection
import com.classprep.junior.ui.planner.PlannerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReviewUiState(
    val loading: Boolean = true,
    val isExample: Boolean = false,
    val date: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    val title: String = "",
    val longDate: String = "",
    val sections: List<ChecklistSection> = emptyList(),
    val remaining: List<ChecklistRow> = emptyList(),
    val ready: Int = 0,
    val total: Int = 0,
    val status: PlanStatus = PlanStatus.OPEN,
    val revision: Int = 0,
    val overLimit: Boolean = false,
    val lessons: List<String> = emptyList(),
    val saving: Boolean = false,
    val message: String? = null,
)

class ReviewViewModel(
    private val source: PlanSource,
    private val clock: AppClock,
    val date: LocalDate,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val _confirmed = MutableStateFlow(false)
    val confirmed: StateFlow<Boolean> = _confirmed

    val state: StateFlow<ReviewUiState> = combine(source.observePlan(date), saving, message) { data, isSaving, msg ->
        val gen = data.generated
        val plan = data.plan
        val today = clock.today()
        val sections = PlannerViewModel.sections(gen.entries, { PlanEngine.isReady(plan, it) }, null)
        val progress = PlanEngine.progress(plan, gen)
        ReviewUiState(
            loading = false,
            isExample = source.isExample,
            date = date,
            today = today,
            title = if (DateSelection.isTomorrow(date, today)) "Ready for tomorrow?" else "Ready for ${DateSelection.dayName(date.dayOfWeek)}?",
            longDate = DateSelection.longDate(date),
            sections = sections,
            remaining = sections.flatMap { it.rows }.filterNot { it.ready },
            ready = progress.ready,
            total = progress.total,
            status = PlanEngine.status(plan),
            revision = plan?.revision ?: 0,
            overLimit = gen.overLimit,
            lessons = gen.lessons.map { "${it.position}. ${it.subjectName}" },
            saving = isSaving,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    init {
        viewModelScope.launch { source.preparePlan(date) }
    }

    /** Guarded against duplicate taps: one in-flight save; the repository also rejects a second confirmation. */
    fun confirm() {
        if (saving.value || _confirmed.value) return
        val s = state.value
        if (s.loading) return
        saving.value = true
        viewModelScope.launch {
            try {
                when (val r = source.confirm(date, s.revision)) {
                    is ConfirmResult.Confirmed -> _confirmed.value = true
                    ConfirmResult.Rejected.ALREADY_CONFIRMED -> _confirmed.value = true
                    ConfirmResult.Rejected.STALE -> message.value = "The list just changed. Please check it again."
                    ConfirmResult.Rejected.INCOMPLETE -> message.value = "A few things still need preparing."
                    ConfirmResult.Rejected.EMPTY -> message.value = "No preparation items added."
                    ConfirmResult.Rejected.OVER_LIMIT -> message.value = "This day has too many items. Ask a parent to shorten the lists."
                }
            } finally {
                saving.value = false
            }
        }
    }
}
