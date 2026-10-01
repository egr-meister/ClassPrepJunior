package com.classprep.junior.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.classprep.junior.domain.reminders.ReminderPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ReminderSettings(
    val enabled: Boolean = false,
    val time: LocalTime = ReminderPolicy.DEFAULT_TIME,
    val permissionRequested: Boolean = false,
)

data class SetupProgress(val completed: Boolean = false, val step: Int = 0, val started: Boolean = false)

/** Small settings and UI preferences. Structured data lives in Room. */
class SettingsStore(private val context: Context) {
    private object Keys {
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderMinutes = intPreferencesKey("reminder_minutes")
        val permissionRequested = booleanPreferencesKey("notification_permission_requested")
        val lastNotifiedTarget = stringPreferencesKey("last_notified_target")
        val setupCompleted = booleanPreferencesKey("setup_completed")
        val setupStep = intPreferencesKey("setup_step")
        val setupStarted = booleanPreferencesKey("setup_started")
    }

    val reminder: Flow<ReminderSettings> = context.dataStore.data.map { p ->
        ReminderSettings(
            enabled = p[Keys.reminderEnabled] ?: false,
            time = p[Keys.reminderMinutes]?.let { LocalTime.of(it / 60, it % 60) } ?: ReminderPolicy.DEFAULT_TIME,
            permissionRequested = p[Keys.permissionRequested] ?: false,
        )
    }

    val setup: Flow<SetupProgress> = context.dataStore.data.map { p ->
        SetupProgress(
            completed = p[Keys.setupCompleted] ?: false,
            step = p[Keys.setupStep] ?: 0,
            started = p[Keys.setupStarted] ?: false,
        )
    }

    suspend fun reminderNow(): ReminderSettings = reminder.first()

    suspend fun setReminderEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.reminderEnabled] = enabled }

    suspend fun setReminderTime(time: LocalTime) =
        context.dataStore.edit { it[Keys.reminderMinutes] = time.hour * 60 + time.minute }

    suspend fun markPermissionRequested() = context.dataStore.edit { it[Keys.permissionRequested] = true }

    suspend fun lastNotifiedTarget(): LocalDate? =
        context.dataStore.data.first()[Keys.lastNotifiedTarget]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    suspend fun setLastNotifiedTarget(date: LocalDate) =
        context.dataStore.edit { it[Keys.lastNotifiedTarget] = date.toString() }

    suspend fun setSetupStep(step: Int) = context.dataStore.edit {
        it[Keys.setupStep] = step
        it[Keys.setupStarted] = true
    }

    suspend fun completeSetup() = context.dataStore.edit {
        it[Keys.setupCompleted] = true
        it[Keys.setupStarted] = true
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}
