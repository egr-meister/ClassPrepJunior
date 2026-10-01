package com.classprep.junior.ui.parent

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classprep.junior.R
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.domain.planning.ItemSuggestions
import com.classprep.junior.domain.planning.Limits
import com.classprep.junior.ui.common.AppIcon
import com.classprep.junior.ui.common.Banner
import com.classprep.junior.ui.common.ConfirmDialog
import com.classprep.junior.ui.common.NotebookSheet
import com.classprep.junior.ui.common.ScreenScaffold
import com.classprep.junior.ui.common.SectionHeader
import com.classprep.junior.ui.common.UiIcon
import com.classprep.junior.ui.common.ruled
import com.classprep.junior.ui.theme.Paper
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

object ParentRoutes {
    const val SUBJECTS = "parent/subjects"
    const val TIMETABLE = "parent/timetable"
    const val ITEMS = "parent/items"
    const val SUBJECT_LINKS = "parent/subjectLinks"
    const val WEEKDAY_LINKS = "parent/weekdayLinks"
    const val TASKS = "parent/tasks"
    const val REMINDERS = "parent/reminders"
    const val HISTORY = "parent/history"
    const val PRIVACY = "parent/privacy"
}

@Composable
private fun NavRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 60.dp).ruled().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted) }
        }
        UiIcon(R.drawable.ic_ui_chevron_right, null)
    }
}

@Composable
fun ParentHomeScreen(vm: ParentViewModel, onBack: () -> Unit, onNavigate: (String) -> Unit, onDataCleared: () -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(title = "Parent area", onBack = onBack) { padding ->
        val config = cfg ?: return@ScreenScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            vm.overLimitDays(config).forEach { (day, n) ->
                Banner(
                    "${DateSelection.dayName(day)} produces $n checklist entries (limit ${Limits.MAX_CHECKLIST_ENTRIES}). Nothing is hidden, but please shorten its lists.",
                    modifier = Modifier.padding(bottom = 8.dp),
                    background = Paper.WarningLight,
                )
            }
            SectionHeader("Timetable")
            NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                NavRow("Subjects", "${config.subjects.size} of ${Limits.MAX_SUBJECTS}") { onNavigate(ParentRoutes.SUBJECTS) }
                NavRow(
                    "Weekly lessons",
                    if (config.schoolDays.isEmpty()) "No school days yet" else config.schoolDays.sorted().joinToString(", ") { DateSelection.shortDayName(it) },
                ) { onNavigate(ParentRoutes.TIMETABLE) }
            }
            SectionHeader("Preparation")
            NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                NavRow("Item library", "${config.items.size} of ${Limits.MAX_ITEMS} reusable items") { onNavigate(ParentRoutes.ITEMS) }
                NavRow("Items for each subject", null) { onNavigate(ParentRoutes.SUBJECT_LINKS) }
                NavRow("Items for each weekday", "General items such as uniform or sports clothes") { onNavigate(ParentRoutes.WEEKDAY_LINKS) }
                NavRow("Homework and date tasks", "One-time tasks for a specific date") { onNavigate(ParentRoutes.TASKS) }
            }
            SectionHeader("Reminders and records")
            NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                NavRow("Evening reminder", if (reminder.enabled) "On, around ${reminder.time}" else "Off") { onNavigate(ParentRoutes.REMINDERS) }
                NavRow("Preparation history", "View or delete confirmed days") { onNavigate(ParentRoutes.HISTORY) }
                NavRow("Privacy", "How your data is stored") { onNavigate(ParentRoutes.PRIVACY) }
            }
            SectionHeader("Reset")
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Clear all local data", color = Paper.Error)
            }
            Text(
                "The parent area is protected by a press-and-hold only to prevent accidental changes. It is not a password.",
                style = MaterialTheme.typography.bodySmall,
                color = Paper.NavyMuted,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear all local data?",
            text = "This deletes the timetable, items, tasks, checklists and history on this device and cancels reminders. It cannot be undone.",
            confirmLabel = "Clear everything",
            destructive = true,
            onConfirm = { confirmClear = false; vm.clearAllData(onDataCleared) },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
fun SubjectsScreen(vm: ParentViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Pair<Subject, List<DayOfWeek>>?>(null) }
    val scope = rememberCoroutineScope()
    ScreenScaffold(
        title = "Subjects",
        onBack = onBack,
        actions = {
            val count = cfg?.subjects?.size ?: 0
            TextButton(onClick = { onEdit(0L) }, enabled = count < Limits.MAX_SUBJECTS) { Text("Add") }
        },
    ) { padding ->
        val config = cfg ?: return@ScreenScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (config.subjects.isEmpty()) Text("No subjects yet. Tap Add to create one.", style = MaterialTheme.typography.bodyLarge)
            if (config.subjects.size >= Limits.MAX_SUBJECTS) Banner("You have reached the limit of ${Limits.MAX_SUBJECTS} subjects.")
            NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                config.subjects.forEach { s ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).ruled().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(s.iconKey, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge)
                            val days = config.lessons.filter { it.subjectId == s.id }.map { it.weekday }.distinct().sorted()
                            Text(
                                if (days.isEmpty()) "Not on the timetable" else days.joinToString(", ") { DateSelection.shortDayName(it) },
                                style = MaterialTheme.typography.bodySmall,
                                color = Paper.NavyMuted,
                            )
                        }
                        IconButton(onClick = { onEdit(s.id) }) { UiIcon(R.drawable.ic_ui_edit, "Edit ${s.name}") }
                        IconButton(onClick = { scope.launch { deleting = s to vm.weekdaysUsing(s.id) } }) {
                            UiIcon(R.drawable.ic_ui_delete, "Delete ${s.name}")
                        }
                    }
                }
            }
        }
    }
    deleting?.let { (s, days) ->
        val usage = if (days.isEmpty()) "It is not used on any weekday." else "It is used on ${days.joinToString(", ") { DateSelection.dayName(it) }}. Those lessons will be removed."
        ConfirmDialog(
            title = "Delete ${s.name}?",
            text = "$usage Its item links are removed too; the items stay in your library. Confirmed plans that change will need confirming again. History is not changed.",
            confirmLabel = "Delete subject",
            destructive = true,
            onConfirm = { deleting = null; vm.deleteSubject(s.id) {} },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun TimetableScreen(vm: ParentViewModel, onBack: () -> Unit, onEditDay: (DayOfWeek) -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    ScreenScaffold(title = "Weekly lessons", onBack = onBack) { padding ->
        val config = cfg ?: return@ScreenScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Choose a day to set it as a school day and order its lessons.", style = MaterialTheme.typography.bodyMedium, color = Paper.NavyMuted)
            Spacer(Modifier.heightIn(min = 8.dp))
            NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                DayOfWeek.entries.forEach { d ->
                    val lessons = config.lessonsFor(d)
                    NavRow(
                        DateSelection.dayName(d),
                        when {
                            !config.isSchoolDay(d) -> "Non-school day"
                            lessons.isEmpty() -> "School day · no lessons yet"
                            else -> lessons.joinToString(", ") { config.subject(it.subjectId)?.name ?: "?" }
                        },
                    ) { onEditDay(d) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemsLibraryScreen(vm: ParentViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit, onAddFromSuggestion: (Int) -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Long?>(null) }
    ScreenScaffold(
        title = "Item library",
        onBack = onBack,
        actions = { TextButton(onClick = { onEdit(0L) }, enabled = (cfg?.items?.size ?: 0) < Limits.MAX_ITEMS) { Text("Add") } },
    ) { padding ->
        val config = cfg ?: return@ScreenScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            SectionHeader("Suggestions")
            Text("Tap a suggestion to add it to your library, then edit if needed.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemSuggestions.all.forEachIndexed { i, s ->
                    AssistChip(
                        onClick = { onAddFromSuggestion(i) },
                        label = { Text(s.name) },
                        leadingIcon = { AppIcon(s.iconKey, null) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            message?.let { Banner(it, background = Paper.WarningLight) }
            ItemCategory.entries.forEach { cat ->
                val items = config.items.filter { it.category == cat }
                if (items.isNotEmpty()) {
                    SectionHeader(cat.label)
                    NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                        items.forEach { item ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).ruled().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(item.iconKey, null)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                    val sub = listOfNotNull(item.note, linkSummary(config, item.id) ?: "Not linked yet").joinToString(" · ")
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
                                }
                                IconButton(onClick = { onEdit(item.id) }) { UiIcon(R.drawable.ic_ui_edit, "Edit ${item.name}") }
                                IconButton(onClick = { deleting = item.id }) { UiIcon(R.drawable.ic_ui_delete, "Delete ${item.name}") }
                            }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { id ->
        val name = cfg?.item(id)?.name ?: "item"
        ConfirmDialog(
            "Delete $name?",
            "It will be removed from every subject and weekday list. Confirmed plans that change will need confirming again. History is not changed.",
            "Delete item",
            onConfirm = { vm.deleteItem(id); deleting = null },
            onDismiss = { deleting = null },
            destructive = true,
        )
    }
}

@Composable
fun LinksIndexScreen(vm: ParentViewModel, bySubject: Boolean, onBack: () -> Unit, onOpenSubject: (Long) -> Unit, onOpenDay: (DayOfWeek) -> Unit) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    ScreenScaffold(title = if (bySubject) "Items for each subject" else "Items for each weekday", onBack = onBack) { padding ->
        val config = cfg ?: return@ScreenScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                if (bySubject) {
                    if (config.subjects.isEmpty()) Text("Add subjects first.", Modifier.padding(16.dp))
                    config.subjects.forEach { s ->
                        val names = config.subjectItems[s.id].orEmpty().mapNotNull { config.item(it)?.name }
                        NavRow(s.name, if (names.isEmpty()) "No items" else names.joinToString(", ")) { onOpenSubject(s.id) }
                    }
                } else {
                    if (config.schoolDays.isEmpty()) Text("Mark school days in Weekly lessons first.", Modifier.padding(16.dp))
                    config.schoolDays.sorted().forEach { d ->
                        val names = config.weekdayItems[d].orEmpty().mapNotNull { config.item(it)?.name }
                        NavRow(DateSelection.dayName(d), if (names.isEmpty()) "No items" else names.joinToString(", ")) { onOpenDay(d) }
                    }
                }
            }
        }
    }
}

@Composable
fun DateTasksScreen(vm: ParentViewModel, onBack: () -> Unit, onEdit: (Long, LocalDate) -> Unit) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Long?>(null) }
    val today = vm.today
    ScreenScaffold(
        title = "Homework and date tasks",
        onBack = onBack,
        actions = { TextButton(onClick = { onEdit(0L, today.plusDays(1)) }) { Text("Add") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("One-time tasks for an actual date (today to two weeks ahead).", style = MaterialTheme.typography.bodyMedium, color = Paper.NavyMuted)
            if (tasks.isEmpty()) Text("No upcoming tasks.", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
            tasks.groupBy { it.targetDate }.toSortedMap().forEach { (date, list) ->
                SectionHeader("${DateSelection.longDate(date)} (${list.size}/${Limits.MAX_TASKS_PER_DATE})")
                NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                    list.forEach { t ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).ruled().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(t.iconKey, null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                Text(t.name, style = MaterialTheme.typography.bodyLarge)
                                Text(listOfNotNull(t.category.label, t.note).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
                            }
                            IconButton(onClick = { onEdit(t.id, t.targetDate) }) { UiIcon(R.drawable.ic_ui_edit, "Edit ${t.name}") }
                            IconButton(onClick = { deleting = t.id }) { UiIcon(R.drawable.ic_ui_delete, "Delete ${t.name}") }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { id ->
        ConfirmDialog("Delete this task?", "It will be removed from that day's checklist.", "Delete",
            onConfirm = { vm.deleteTask(id); deleting = null }, onDismiss = { deleting = null }, destructive = true)
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "Privacy", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                "Everything stays on this device" to "Your timetable, items, tasks, checklists and history are stored only in this app's private storage. ClassPrep Junior has no account, no server and no internet permission.",
                "No personal details needed" to "No school login, student name, contact details or other personal identifiers are requested.",
                "No backup or transfer" to "Cloud backup and device-to-device transfer are turned off for this app, so the data does not leave the phone that way. Uninstalling the app deletes it.",
                "Local reminders only" to "The optional evening reminder is created on this device by Android's alarm service. It shows a general message and never displays homework text on the lock screen.",
                "No tracking" to "There are no adverts, analytics, payments, location, contacts or calendar access.",
                "Your control" to "Parents can delete individual history entries, clear history, or clear all local data at any time in the parent area.",
                "Ticks are self-reported" to "Ticking an item records that the child reports it as prepared. The app cannot check bags and does not mark homework.",
            ).forEach { (title, body) ->
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
