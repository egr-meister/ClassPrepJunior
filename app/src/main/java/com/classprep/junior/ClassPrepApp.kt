package com.classprep.junior

import android.app.Application
import android.content.Context
import com.classprep.junior.data.local.AppDatabase
import com.classprep.junior.data.prefs.SettingsStore
import com.classprep.junior.data.reminders.ReminderNotifier
import com.classprep.junior.data.reminders.ReminderScheduler
import com.classprep.junior.data.repository.ExamplePlanSource
import com.classprep.junior.data.repository.PlannerRepository
import com.classprep.junior.domain.AppClock
import com.classprep.junior.domain.SystemAppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClassPrepApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannel()
        // Opening the app recovers reminders after a force-stop and applies any time-zone change.
        container.appScope.launch { container.reminderScheduler.reschedule() }
    }
}

/** Manual dependency injection. */
class AppContainer(context: Context, val clock: AppClock = SystemAppClock) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    val settings = SettingsStore(appContext)
    val notifier = ReminderNotifier(appContext)

    val repository: PlannerRepository by lazy {
        PlannerRepository(database, clock) { reminderScheduler.reschedule() }
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext, settings, { repository }, notifier, clock)
    }

    /** Fresh, isolated example each time the example is opened. */
    var example: ExamplePlanSource = ExamplePlanSource(clock)
        private set

    fun resetExample() {
        example = ExamplePlanSource(clock)
    }

    private val _timeVersion = MutableStateFlow(0L)

    /** Bumped on resume, date, time and time-zone changes so date labels are recomputed. */
    val timeVersion: StateFlow<Long> = _timeVersion.asStateFlow()

    fun timeChanged() {
        _timeVersion.value = _timeVersion.value + 1
    }

    /** Cancels reminders, deletes all user data and history, and returns the app to initial setup. */
    suspend fun clearAllData() {
        reminderScheduler.cancel()
        notifier.cancelPosted()
        repository.clearAllTables()
        settings.clearAll()
        resetExample()
    }
}
