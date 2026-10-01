package com.classprep.junior.data.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.classprep.junior.MainActivity
import com.classprep.junior.R
import java.time.LocalDate

enum class NotificationStatus { ALLOWED, PERMISSION_NOT_GRANTED, APP_BLOCKED, CHANNEL_BLOCKED }

/** Posts the evening reminder. Content is generic: no homework text or other user data on the lock screen. */
class ReminderNotifier(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "evening_reminder"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_TARGET_DATE = "com.classprep.junior.extra.TARGET_DATE"
    }

    fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)!!
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun status(): NotificationStatus {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return NotificationStatus.PERMISSION_NOT_GRANTED
        }
        val nmc = NotificationManagerCompat.from(context)
        if (!nmc.areNotificationsEnabled()) return NotificationStatus.APP_BLOCKED
        val channel = context.getSystemService(NotificationManager::class.java)!!.getNotificationChannel(CHANNEL_ID)
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return NotificationStatus.CHANNEL_BLOCKED
        return NotificationStatus.ALLOWED
    }

    /** Opens tomorrow's plan (explicit, immutable). MainActivity is the root, so Back leaves the app normally. */
    fun contentIntent(targetDate: LocalDate): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.classprep.junior.action.OPEN_PLAN"
            putExtra(EXTRA_TARGET_DATE, targetDate.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 2001, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    @SuppressLint("MissingPermission") // Checked in status().
    fun post(targetDate: LocalDate): Boolean {
        ensureChannel()
        if (status() != NotificationStatus.ALLOWED) return false
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notebook)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(targetDate))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        return true
    }

    fun cancelPosted() = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    fun appNotificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun channelSettingsIntent(): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
