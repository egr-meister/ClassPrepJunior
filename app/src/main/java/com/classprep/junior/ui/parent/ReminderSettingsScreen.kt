package com.classprep.junior.ui.parent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classprep.junior.R
import com.classprep.junior.data.reminders.NotificationStatus
import com.classprep.junior.domain.reminders.ReminderPolicy
import com.classprep.junior.ui.common.Banner
import com.classprep.junior.ui.common.ScreenScaffold
import com.classprep.junior.ui.common.UiIcon
import com.classprep.junior.ui.theme.Paper
import java.time.LocalTime

@Composable
fun ReminderSettingsContent(vm: ParentViewModel, modifier: Modifier = Modifier) {
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val status by vm.notificationStatus.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshNotificationStatus() }
    LifecycleResumeEffect(Unit) {
        vm.refreshNotificationStatus()
        onPauseOrDispose { }
    }

    fun enable(on: Boolean) {
        vm.setReminderEnabled(on)
        if (on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !reminder.permissionRequested
        ) {
            // Asked once, only after the parent turns reminders on. Never re-prompted automatically.
            vm.markPermissionRequested()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(reminder.enabled, role = Role.Switch) { enable(it) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Evening reminder", style = MaterialTheme.typography.titleMedium)
                Text("Off by default. One reminder the evening before a school day.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
            }
            Switch(checked = reminder.enabled, onCheckedChange = null)
        }
        if (reminder.enabled) {
            if (status != NotificationStatus.ALLOWED) {
                Banner(
                    when (status) {
                        NotificationStatus.CHANNEL_BLOCKED -> "Notifications are not allowed. The “Evening preparation reminder” category is turned off in Android settings."
                        else -> "Notifications are not allowed. Planning still works normally; reminders just won't appear."
                    },
                    background = Paper.WarningLight,
                ) {
                    TextButton(onClick = {
                        runCatching { context.startActivity(vm.notificationSettingsIntent(status == NotificationStatus.CHANNEL_BLOCKED)) }
                    }) { Text("Open notification settings") }
                }
            }
            Text("Time", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.shiftReminderTime(-15) }, enabled = reminder.time > ReminderPolicy.EARLIEST_TIME, modifier = Modifier.heightIn(min = 48.dp)) { Text("−15 min") }
                Text("Around ${reminder.time}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedButton(onClick = { vm.shiftReminderTime(15) }, enabled = reminder.time < ReminderPolicy.LATEST_TIME, modifier = Modifier.heightIn(min = 48.dp)) { Text("+15 min") }
            }
            QuickTimes(reminder.time) { vm.setReminderTime(it) }
            Text(
                "The time is approximate: Android may delay reminders to save battery. Late reminders are skipped rather than shown at night. " +
                    "No reminder is shown if tomorrow isn't a school day, has no items, or is already confirmed ready. " +
                    "If the app is force-stopped, reminders stop until the app is opened again.",
                style = MaterialTheme.typography.bodySmall,
                color = Paper.NavyMuted,
            )
            Text("Preview", style = MaterialTheme.typography.titleMedium)
            Surface(shape = RoundedCornerShape(14.dp), color = Paper.Sheet, border = BorderStroke(1.dp, Paper.Rule)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    UiIcon(R.drawable.ic_ui_bell, null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Prepare for tomorrow", style = MaterialTheme.typography.titleSmall)
                        Text("Your school checklist is ready to review.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            FlowRow2 {
                OutlinedButton(onClick = vm::sendPreview, modifier = Modifier.heightIn(min = 48.dp)) { Text("Show preview notification") }
                OutlinedButton(
                    onClick = { runCatching { context.startActivity(vm.notificationSettingsIntent(false)) } },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Android notification settings") }
                if (vm.isDebug) {
                    OutlinedButton(onClick = vm::debugTestReminder, modifier = Modifier.heightIn(min = 48.dp)) { Text("Debug: test in ~1 min") }
                }
            }
            message?.let { Banner(it) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow2(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickTimes(current: LocalTime, onPick: (LocalTime) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(LocalTime.of(18, 0), LocalTime.of(19, 0), LocalTime.of(20, 0)).forEach { t ->
            FilterChip(selected = current == t, onClick = { onPick(t) }, label = { Text(t.toString()) }, modifier = Modifier.heightIn(min = 48.dp))
        }
    }
}

@Composable
fun ReminderSettingsScreen(vm: ParentViewModel, onBack: () -> Unit) {
    ScreenScaffold(title = "Evening reminder", onBack = onBack) { padding ->
        ReminderSettingsContent(vm, Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp))
    }
}
