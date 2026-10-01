package com.classprep.junior.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.classprep.junior.data.prefs.SettingsStore
import com.classprep.junior.data.repository.PlannerRepository
import com.classprep.junior.domain.AppClock
import com.classprep.junior.domain.reminders.ReminderPolicy
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Schedules a single inexact one-shot alarm (setAndAllowWhileIdle) for the next eligible evening.
 * No exact-alarm permission, no wake lock, no foreground service. Delivery time is approximate.
 */
class ReminderScheduler(
    private val context: Context,
    private val settings: SettingsStore,
    private val repository: () -> PlannerRepository,
    private val notifier: ReminderNotifier,
    private val clock: AppClock,
) {
    companion object {
        private const val TAG = "ClassPrepReminder"
        private const val REQUEST_CODE = 3001
        const val ACTION_REMINDER = "com.classprep.junior.action.EVENING_REMINDER"
        const val EXTRA_EVENING = "evening"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_TEST = "test"
    }

    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)!!

    private fun pendingIntent(evening: LocalDate?, time: LocalTime?, test: Boolean, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            evening?.let { putExtra(EXTRA_EVENING, it.toString()) }
            time?.let { putExtra(EXTRA_MINUTES, it.hour * 60 + it.minute) }
            putExtra(EXTRA_TEST, test)
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancel() {
        pendingIntent(null, null, false, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /** Recomputes the next evening from settings and the timetable. */
    suspend fun reschedule(notBeforeEvening: LocalDate? = null): ZonedDateTime? {
        val s = settings.reminderNow()
        if (!s.enabled) {
            cancel()
            Log.i(TAG, "Reminder disabled; nothing scheduled")
            return null
        }
        val cfg = repository().loadConfig()
        val next = ReminderPolicy.nextEvening(clock.now(), s.time, cfg.schoolDays, notBeforeEvening)
        if (next == null) {
            cancel()
            Log.i(TAG, "No school days configured; nothing scheduled")
            return null
        }
        set(next, s.time, test = false)
        return next
    }

    private fun set(at: ZonedDateTime, time: LocalTime, test: Boolean) {
        val pi = pendingIntent(at.toLocalDate(), time, test, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pi)
        Log.i(TAG, "Inexact reminder scheduled around $at (test=$test)")
    }

    /** Debug-build verification helper: an inexact alarm roughly one minute from now for tomorrow's plan. */
    fun scheduleTestInOneMinute() {
        val at = clock.now().plusMinutes(1).withSecond(0).withNano(0)
        set(at, at.toLocalTime(), test = true)
    }

    /** Called by the receiver. Short work only: evaluate, post, schedule the next occurrence. */
    suspend fun onAlarm(evening: LocalDate?, minutes: Int?, test: Boolean) {
        val s = settings.reminderNow()
        if (!s.enabled && !test) {
            cancel()
            return
        }
        val now = clock.now()
        val time = minutes?.let { LocalTime.of(it / 60, it % 60) } ?: s.time
        val eveningDate = evening ?: now.toLocalDate()
        if (ReminderPolicy.isStale(now, eveningDate, time)) {
            Log.i(TAG, "Skipping stale reminder for evening $eveningDate (now $now)")
        } else {
            val target = eveningDate.plusDays(1)
            val repo = repository()
            val cfg = repo.loadConfig()
            val generated = repo.generate(target)
            val status = repo.planStatus(target)
            val lastNotified = if (test) null else settings.lastNotifiedTarget()
            val e = ReminderPolicy.eligibility(target, cfg.schoolDays, generated.entries.size, status, lastNotified)
            Log.i(TAG, "Reminder for $target: ${e.reason}")
            if (e.eligible && notifier.post(target) && !test) settings.setLastNotifiedTarget(target)
        }
        if (s.enabled) reschedule(notBeforeEvening = if (test) null else eveningDate.plusDays(1))
    }
}
