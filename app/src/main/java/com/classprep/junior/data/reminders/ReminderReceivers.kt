package com.classprep.junior.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.classprep.junior.ClassPrepApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Runs a short suspending block with goAsync(); bounded well under the broadcast time limit. */
private fun BroadcastReceiver.runShort(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            withTimeoutOrNull(8_000) { block() } ?: Log.w("ClassPrepReminder", "Receiver work timed out")
        } catch (t: Throwable) {
            Log.e("ClassPrepReminder", "Receiver failed", t)
        } finally {
            pending.finish()
        }
    }
}

/** Target of our own explicit alarm PendingIntent. Not exported. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMINDER) return
        val container = (context.applicationContext as ClassPrepApp).container
        val evening = intent.getStringExtra(ReminderScheduler.EXTRA_EVENING)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val minutes = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, -1).takeIf { it >= 0 }
        val test = intent.getBooleanExtra(ReminderScheduler.EXTRA_TEST, false)
        runShort { container.reminderScheduler.onAlarm(evening, minutes, test) }
    }
}

/** Reboot, app update, time and time-zone changes: recompute the next evening. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    private val handled = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in handled) return
        val container = (context.applicationContext as ClassPrepApp).container
        runShort {
            container.timeChanged()
            container.reminderScheduler.reschedule()
        }
    }
}
