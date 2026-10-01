package com.classprep.junior.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classprep.junior.AppContainer
import com.classprep.junior.data.prefs.ReminderSettings
import com.classprep.junior.data.reminders.NotificationStatus
import com.classprep.junior.domain.model.DateTask
import com.classprep.junior.domain.model.IconKeys
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.ReusableItem
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.model.WeekConfig
import com.classprep.junior.domain.planning.ChecklistGenerator
import com.classprep.junior.domain.planning.ConfigEdit
import com.classprep.junior.domain.planning.ItemSuggestions
import com.classprep.junior.domain.planning.Limits
import com.classprep.junior.domain.planning.Validation
import com.classprep.junior.domain.reminders.ReminderPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Shared read model for parent screens. */
class ParentViewModel(private val c: AppContainer) : ViewModel() {
    val config: StateFlow<WeekConfig?> = c.repository.observeConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reminder: StateFlow<ReminderSettings> = c.settings.reminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    val tasks: StateFlow<List<DateTask>> = c.repository.observeTasksFrom(c.clock.today())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _notificationStatus = MutableStateFlow(c.notifier.status())
    val notificationStatus: StateFlow<NotificationStatus> = _notificationStatus.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val today: LocalDate get() = c.clock.today()
    val isDebug = com.classprep.junior.BuildConfig.DEBUG

    fun refreshNotificationStatus() {
        _notificationStatus.value = c.notifier.status()
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Weekdays whose generated checklist exceeds the daily limit (parent-facing validation). */
    fun overLimitDays(cfg: WeekConfig): List<Pair<DayOfWeek, Int>> = cfg.schoolDays.sorted().mapNotNull { d ->
        val date = nextDateFor(d)
        val n = ChecklistGenerator.generate(date, cfg, tasks.value).entries.size
        if (n > Limits.MAX_CHECKLIST_ENTRIES) d to n else null
    }

    private fun nextDateFor(day: DayOfWeek): LocalDate {
        var d = today.plusDays(1)
        while (d.dayOfWeek != day) d = d.plusDays(1)
        return d
    }

    // ----- Subjects -----
    fun deleteSubject(id: Long, onDone: () -> Unit) = viewModelScope.launch {
        c.repository.deleteSubject(id)
        onDone()
    }

    suspend fun weekdaysUsing(id: Long): List<DayOfWeek> = c.repository.weekdaysUsingSubject(id)

    suspend fun affected(edit: ConfigEdit): Int = c.repository.confirmedPlansAffectedBy(edit)

    // ----- Items -----
    fun deleteItem(id: Long) = viewModelScope.launch { c.repository.deleteItem(id) }

    fun addSuggestion(s: ItemSuggestions.Suggestion) = viewModelScope.launch {
        runCatching { c.repository.saveItem(ReusableItem(0, s.name, null, s.iconKey, s.category)) }
            .onFailure { _message.value = it.message }
    }

    // ----- Tasks -----
    fun deleteTask(id: Long) = viewModelScope.launch { c.repository.deleteTask(id) }

    // ----- Reminders -----
    fun setReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        c.settings.setReminderEnabled(enabled)
        c.reminderScheduler.reschedule()
        refreshNotificationStatus()
    }

    fun markPermissionRequested() = viewModelScope.launch { c.settings.markPermissionRequested() }

    fun shiftReminderTime(minutes: Long) = viewModelScope.launch {
        val current = c.settings.reminder.first().time
        c.settings.setReminderTime(ReminderPolicy.clampTime(current.plusMinutes(minutes)))
        c.reminderScheduler.reschedule()
    }

    fun setReminderTime(time: LocalTime) = viewModelScope.launch {
        c.settings.setReminderTime(ReminderPolicy.clampTime(time))
        c.reminderScheduler.reschedule()
    }

    fun sendPreview() {
        refreshNotificationStatus()
        if (!c.notifier.post(today.plusDays(1))) _message.value = "Notifications are not allowed."
    }

    fun debugTestReminder() {
        c.reminderScheduler.scheduleTestInOneMinute()
        _message.value = "Test reminder scheduled for about one minute from now (approximate)."
    }

    fun notificationSettingsIntent(channel: Boolean) =
        if (channel) c.notifier.channelSettingsIntent() else c.notifier.appNotificationSettingsIntent()

    // ----- Reset -----
    fun clearAllData(onDone: () -> Unit) = viewModelScope.launch {
        c.clearAllData()
        onDone()
    }
}

/** Generic draft holder: keeps the original and the edited value so forms can detect unsaved changes. */
abstract class DraftViewModel<T> : ViewModel() {
    protected val _initial = MutableStateFlow<T?>(null)
    protected val _draft = MutableStateFlow<T?>(null)
    val draft: StateFlow<T?> = _draft.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isDirty: Boolean get() = _initial.value != null && _draft.value != _initial.value

    fun update(transform: (T) -> T) {
        _draft.update { it?.let(transform) }
        _error.value = null
    }

    protected fun initDraft(value: T) {
        if (_initial.value == null) {
            _initial.value = value
            _draft.value = value
        }
    }

    protected fun setError(msg: String?) {
        _error.value = msg
    }

    protected fun launchSave(onDone: () -> Unit, block: suspend () -> String?) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val err = try {
                block()
            } catch (e: IllegalStateException) {
                e.message ?: "Could not save."
            } catch (e: IllegalArgumentException) {
                e.message ?: "Could not save."
            }
            _saving.value = false
            if (err == null) {
                _initial.value = _draft.value
                onDone()
            } else {
                _error.value = err
            }
        }
    }
}

data class SubjectDraft(val name: String, val iconKey: String, val iconTouched: Boolean)

class SubjectEditorViewModel(private val c: AppContainer, val subjectId: Long) : DraftViewModel<SubjectDraft>() {
    private var original: Subject? = null
    var existingNames: List<String> = emptyList()
        private set

    init {
        viewModelScope.launch {
            val cfg = c.repository.loadConfig()
            original = cfg.subject(subjectId)
            existingNames = cfg.subjects.filter { it.id != subjectId }.map { it.name }
            val o = original
            initDraft(SubjectDraft(o?.name ?: "", o?.iconKey ?: IconKeys.SUBJECT_GENERIC, o != null))
        }
    }

    fun validate(d: SubjectDraft): String? = Validation.subjectName(d.name, existingNames)

    fun edit(d: SubjectDraft): ConfigEdit = ConfigEdit.UpsertSubject(
        Subject(subjectId, d.name.trim(), d.iconKey, original?.displayOrder ?: Int.MAX_VALUE),
    )

    suspend fun affected(): Int = draft.value?.let { if (subjectId == 0L) 0 else c.repository.confirmedPlansAffectedBy(edit(it)) } ?: 0

    fun save(onDone: () -> Unit) {
        val d = draft.value ?: return
        validate(d)?.let { setError(it); return }
        launchSave(onDone) {
            c.repository.saveSubject(Subject(subjectId, d.name.trim(), d.iconKey, original?.displayOrder ?: 0))
            null
        }
    }
}

data class DayDraft(val isSchoolDay: Boolean, val subjectIds: List<Long>)

class DayEditorViewModel(private val c: AppContainer, val day: DayOfWeek) : DraftViewModel<DayDraft>() {
    val config: StateFlow<WeekConfig?> = c.repository.observeConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var weekdayItemCount = 0
        private set

    init {
        viewModelScope.launch {
            val cfg = c.repository.loadConfig()
            weekdayItemCount = cfg.weekdayItems[day].orEmpty().size
            initDraft(DayDraft(cfg.isSchoolDay(day), cfg.lessonsFor(day).map { it.subjectId }))
        }
    }

    fun copyFrom(other: DayOfWeek) {
        val cfg = config.value ?: return
        update { it.copy(isSchoolDay = true, subjectIds = cfg.lessonsFor(other).map { l -> l.subjectId }) }
    }

    fun edit(d: DayDraft): ConfigEdit = ConfigEdit.SetWeekday(day, d.isSchoolDay, d.subjectIds)

    suspend fun affected(): Int = draft.value?.let { c.repository.confirmedPlansAffectedBy(edit(it)) } ?: 0

    fun save(onDone: () -> Unit) {
        val d = draft.value ?: return
        if (d.subjectIds.size > Limits.MAX_LESSONS_PER_DAY) {
            setError("Up to ${Limits.MAX_LESSONS_PER_DAY} lessons per day.")
            return
        }
        launchSave(onDone) {
            c.repository.saveWeekday(day, d.isSchoolDay, if (d.isSchoolDay) d.subjectIds else emptyList())
            null
        }
    }
}

data class ItemDraft(val name: String, val note: String, val iconKey: String, val category: ItemCategory)

class ItemEditorViewModel(private val c: AppContainer, val itemId: Long, suggestionIndex: Int) : DraftViewModel<ItemDraft>() {
    init {
        viewModelScope.launch {
            val cfg = c.repository.loadConfig()
            val item = cfg.item(itemId)
            val s = ItemSuggestions.all.getOrNull(suggestionIndex)
            initDraft(
                when {
                    item != null -> ItemDraft(item.name, item.note ?: "", item.iconKey, item.category)
                    s != null -> ItemDraft(s.name, "", s.iconKey, s.category)
                    else -> ItemDraft("", "", IconKeys.ITEM_BAG, ItemCategory.OTHER)
                },
            )
        }
    }

    fun validate(d: ItemDraft): String? = Validation.itemName(d.name) ?: Validation.note(d.note)

    private fun toItem(d: ItemDraft) = ReusableItem(itemId, d.name.trim(), Validation.normalizedNote(d.note), d.iconKey, d.category)

    suspend fun affected(): Int = draft.value?.let { if (itemId == 0L) 0 else c.repository.confirmedPlansAffectedBy(ConfigEdit.UpsertItem(toItem(it))) } ?: 0

    fun save(onDone: () -> Unit) {
        val d = draft.value ?: return
        validate(d)?.let { setError(it); return }
        launchSave(onDone) {
            c.repository.saveItem(toItem(d))
            null
        }
    }
}

/** Selection of items linked to a subject (subjectId != null) or to a weekday. */
class LinksEditorViewModel(private val c: AppContainer, val subjectId: Long?, val day: DayOfWeek?) : DraftViewModel<Set<Long>>() {
    val config: StateFlow<WeekConfig?> = c.repository.observeConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val cfg = c.repository.loadConfig()
            initDraft(if (subjectId != null) cfg.subjectItems[subjectId].orEmpty() else cfg.weekdayItems[day!!].orEmpty())
        }
    }

    private fun edit(ids: Set<Long>): ConfigEdit =
        if (subjectId != null) ConfigEdit.SetSubjectItems(subjectId, ids) else ConfigEdit.SetWeekdayItems(day!!, ids)

    suspend fun affected(): Int = draft.value?.let { c.repository.confirmedPlansAffectedBy(edit(it)) } ?: 0

    fun toggle(id: Long) = update { if (id in it) it - id else it + id }

    fun save(onDone: () -> Unit) {
        val ids = draft.value ?: return
        launchSave(onDone) {
            if (subjectId != null) c.repository.setSubjectItems(subjectId, ids) else c.repository.setWeekdayItems(day!!, ids)
            null
        }
    }
}

data class TaskDraft(val date: LocalDate, val name: String, val note: String, val iconKey: String, val category: ItemCategory)

class TaskEditorViewModel(private val c: AppContainer, val taskId: Long, date: LocalDate) : DraftViewModel<TaskDraft>() {
    val today: LocalDate = c.clock.today()

    init {
        viewModelScope.launch {
            val t = if (taskId != 0L) c.repository.task(taskId) else null
            initDraft(
                t?.let { TaskDraft(it.targetDate, it.name, it.note ?: "", it.iconKey, it.category) }
                    ?: TaskDraft(date, "", "", IconKeys.ITEM_TASK, ItemCategory.HOMEWORK),
            )
        }
    }

    suspend fun isConfirmed(): Boolean = draft.value?.let { c.repository.isConfirmed(it.date) } ?: false

    fun save(onDone: () -> Unit) {
        val d = draft.value ?: return
        (Validation.itemName(d.name) ?: Validation.note(d.note))?.let { setError(it); return }
        launchSave(onDone) {
            c.repository.saveTask(DateTask(taskId, d.date, d.name, Validation.normalizedNote(d.note), d.iconKey, d.category))
        }
    }
}
