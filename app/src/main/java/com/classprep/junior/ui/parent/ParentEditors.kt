package com.classprep.junior.ui.parent

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.classprep.junior.domain.model.IconKeys
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.domain.planning.Limits
import com.classprep.junior.ui.common.AppIcon
import com.classprep.junior.ui.common.Banner
import com.classprep.junior.ui.common.ConfirmDialog
import com.classprep.junior.ui.common.EditorScaffold
import com.classprep.junior.ui.common.IconField
import com.classprep.junior.ui.common.IconPickerDialog
import com.classprep.junior.ui.common.NotebookSheet
import com.classprep.junior.ui.common.SectionHeader
import com.classprep.junior.ui.common.UiIcon
import com.classprep.junior.ui.common.ruled
import com.classprep.junior.ui.theme.Paper
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/** Explains, before saving, that already-confirmed future plans will need confirming again. */
@Composable
fun ImpactDialog(count: Int?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (count == null) return
    ConfirmDialog(
        title = "Update confirmed plans?",
        text = "This change affects $count day${if (count == 1) "" else "s"} already confirmed ready. " +
            "Ticks for unchanged items are kept, new items start unticked, and the day will need to be confirmed again. " +
            "Past history stays as it was.",
        confirmLabel = "Save changes",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
fun SubjectEditorScreen(vm: SubjectEditorViewModel, onClose: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf(false) }
    var impact by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val d = draft
    EditorScaffold(
        title = if (vm.subjectId == 0L) "Add subject" else "Edit subject",
        dirty = vm.isDirty,
        saveEnabled = d != null && d.name.isNotBlank(),
        saving = saving,
        onSave = { scope.launch { val n = vm.affected(); if (n > 0) impact = n else vm.save(onClose) } },
        onClose = onClose,
    ) { padding ->
        if (d == null) return@EditorScaffold
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = d.name,
                onValueChange = { v ->
                    if (v.length <= Limits.SUBJECT_NAME_MAX) {
                        vm.update { it.copy(name = v, iconKey = if (it.iconTouched) it.iconKey else IconKeys.suggestSubjectIcon(v)) }
                    }
                },
                label = { Text("Subject name") },
                supportingText = { Text(error ?: "${d.name.trim().length}/${Limits.SUBJECT_NAME_MAX}") },
                isError = error != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            IconField("Choose icon", d.iconKey) { picker = true }
        }
    }
    if (picker && d != null) {
        IconPickerDialog("Subject icon", IconKeys.subjectOptions, d.iconKey, onPick = { k -> vm.update { it.copy(iconKey = k, iconTouched = true) }; picker = false }, onDismiss = { picker = false })
    }
    ImpactDialog(impact, onConfirm = { impact = null; vm.save(onClose) }, onDismiss = { impact = null })
}

@Composable
fun DayEditorScreen(vm: DayEditorViewModel, onClose: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var confirmNonSchool by rememberSaveable { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var copyMenu by remember { mutableStateOf(false) }
    var changeSlot by remember { mutableStateOf<Int?>(null) }
    var impact by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val d = draft
    val config = cfg
    EditorScaffold(
        title = DateSelection.dayName(vm.day),
        dirty = vm.isDirty,
        saveEnabled = d != null,
        saving = saving,
        onSave = { scope.launch { val n = vm.affected(); if (n > 0) impact = n else vm.save(onClose) } },
        onClose = onClose,
    ) { padding ->
        if (d == null || config == null) return@EditorScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(d.isSchoolDay, role = Role.Switch) { on ->
                    if (!on && (d.subjectIds.isNotEmpty() || vm.weekdayItemCount > 0)) confirmNonSchool = true
                    else vm.update { it.copy(isSchoolDay = on) }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("School day", style = MaterialTheme.typography.titleMedium)
                    Text(if (d.isSchoolDay) "Lessons are planned on this day." else "Non-school day: no lessons or recurring items.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
                }
                Switch(checked = d.isSchoolDay, onCheckedChange = null)
            }
            if (d.isSchoolDay) {
                SectionHeader("Lessons (${d.subjectIds.size}/${Limits.MAX_LESSONS_PER_DAY})")
                if (config.subjects.isEmpty()) Banner("Add subjects first, then assign them to this day.")
                NotebookSheet(Modifier.fillMaxWidth()) {
                    if (d.subjectIds.isEmpty()) {
                        Text("No lessons yet.", Modifier.padding(start = 56.dp, top = 12.dp, bottom = 12.dp), color = Paper.NavyMuted)
                    }
                    d.subjectIds.forEachIndexed { index, sid ->
                        val subject = config.subject(sid)
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).ruled(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", Modifier.width(44.dp).padding(start = 16.dp), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            TextButton(onClick = { changeSlot = index }, modifier = Modifier.weight(1f)) {
                                subject?.let { AppIcon(it.iconKey, null) }
                                Spacer(Modifier.width(8.dp))
                                Text(subject?.name ?: "Choose subject", modifier = Modifier.weight(1f))
                            }
                            IconButton(onClick = { vm.update { it.copy(subjectIds = it.subjectIds.move(index, -1)) } }, enabled = index > 0) {
                                UiIcon(R.drawable.ic_ui_up, "Move lesson ${index + 1} up", tint = if (index > 0) Paper.Navy else Paper.Rule)
                            }
                            IconButton(onClick = { vm.update { it.copy(subjectIds = it.subjectIds.move(index, 1)) } }, enabled = index < d.subjectIds.lastIndex) {
                                UiIcon(R.drawable.ic_ui_down, "Move lesson ${index + 1} down", tint = if (index < d.subjectIds.lastIndex) Paper.Navy else Paper.Rule)
                            }
                            IconButton(onClick = { vm.update { it.copy(subjectIds = it.subjectIds.filterIndexed { i, _ -> i != index }) } }) {
                                UiIcon(R.drawable.ic_ui_delete, "Remove lesson ${index + 1}")
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        OutlinedButton(
                            onClick = { addMenu = true },
                            enabled = config.subjects.isNotEmpty() && d.subjectIds.size < Limits.MAX_LESSONS_PER_DAY,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { UiIcon(R.drawable.ic_ui_add, null); Spacer(Modifier.width(6.dp)); Text("Add lesson") }
                        DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                            config.subjects.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, leadingIcon = { AppIcon(s.iconKey, null) }, onClick = {
                                    addMenu = false
                                    vm.update { it.copy(subjectIds = it.subjectIds + s.id) }
                                })
                            }
                        }
                    }
                    Column {
                        OutlinedButton(onClick = { copyMenu = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Copy from…") }
                        DropdownMenu(expanded = copyMenu, onDismissRequest = { copyMenu = false }) {
                            DayOfWeek.entries.filter { it != vm.day }.forEach { other ->
                                val n = config.lessonsFor(other).size
                                DropdownMenuItem(
                                    text = { Text("${DateSelection.dayName(other)} ($n lessons)") },
                                    enabled = n > 0,
                                    onClick = { copyMenu = false; vm.copyFrom(other) },
                                )
                            }
                        }
                    }
                }
                Text("A subject can appear more than once. Its items are listed only once on the checklist.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
            }
            error?.let { Banner(it, background = Paper.WarningLight) }
        }
    }
    changeSlot?.let { slot ->
        if (config != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { changeSlot = null },
                containerColor = Paper.Sheet,
                title = { Text("Lesson ${slot + 1}") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        config.subjects.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                                    vm.update { it.copy(subjectIds = it.subjectIds.mapIndexed { i, v -> if (i == slot) s.id else v }) }
                                    changeSlot = null
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) { AppIcon(s.iconKey, null); Spacer(Modifier.width(10.dp)); Text(s.name) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { changeSlot = null }) { Text("Cancel") } },
            )
        }
    }
    if (confirmNonSchool) {
        ConfirmDialog(
            title = "Make ${DateSelection.dayName(vm.day)} a non-school day?",
            text = "When you save, its ${d?.subjectIds?.size ?: 0} lesson(s) and ${vm.weekdayItemCount} recurring weekday item link(s) will be removed.",
            confirmLabel = "Make non-school",
            destructive = true,
            onConfirm = { confirmNonSchool = false; vm.update { it.copy(isSchoolDay = false, subjectIds = emptyList()) } },
            onDismiss = { confirmNonSchool = false },
        )
    }
    ImpactDialog(impact, onConfirm = { impact = null; vm.save(onClose) }, onDismiss = { impact = null })
}

private fun <T> List<T>.move(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (target !in indices) return this
    val m = toMutableList()
    val v = m.removeAt(index)
    m.add(target, v)
    return m
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemEditorScreen(vm: ItemEditorViewModel, onClose: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf(false) }
    var impact by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val d = draft
    EditorScaffold(
        title = if (vm.itemId == 0L) "Add item" else "Edit item",
        dirty = vm.isDirty || (vm.itemId == 0L && d != null && d.name.isNotBlank()),
        saveEnabled = d != null && d.name.isNotBlank(),
        saving = saving,
        onSave = { scope.launch { val n = vm.affected(); if (n > 0) impact = n else vm.save(onClose) } },
        onClose = onClose,
    ) { padding ->
        if (d == null) return@EditorScaffold
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = d.name,
                onValueChange = { v -> if (v.length <= Limits.ITEM_NAME_MAX) vm.update { it.copy(name = v) } },
                label = { Text("Item name") },
                supportingText = { Text("${d.name.trim().length}/${Limits.ITEM_NAME_MAX}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = d.note,
                onValueChange = { v -> if (v.length <= Limits.NOTE_MAX) vm.update { it.copy(note = v) } },
                label = { Text("Note (optional), e.g. “blue cover”") },
                supportingText = { Text("${d.note.length}/${Limits.NOTE_MAX}") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Category", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemCategory.entries.filter { it != ItemCategory.HOMEWORK }.forEach { cat ->
                    FilterChip(selected = d.category == cat, onClick = { vm.update { it.copy(category = cat) } }, label = { Text(cat.label) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            IconField("Choose icon", d.iconKey) { picker = true }
            Text("Items with the same name are kept separate. Use a note to tell them apart.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
            error?.let { Banner(it, background = Paper.WarningLight) }
        }
    }
    if (picker && d != null) {
        IconPickerDialog("Item icon", IconKeys.itemOptions, d.iconKey, onPick = { k -> vm.update { it.copy(iconKey = k) }; picker = false }, onDismiss = { picker = false })
    }
    ImpactDialog(impact, onConfirm = { impact = null; vm.save(onClose) }, onDismiss = { impact = null })
}

@Composable
fun LinksEditorScreen(vm: LinksEditorViewModel, onClose: () -> Unit, onAddItem: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    var impact by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val config = cfg
    val title = when {
        vm.subjectId != null -> "Items for ${config?.subject(vm.subjectId)?.name ?: "subject"}"
        else -> "Items for every ${DateSelection.dayName(vm.day!!)}"
    }
    EditorScaffold(
        title = title,
        dirty = vm.isDirty,
        saveEnabled = draft != null,
        saving = saving,
        onSave = { scope.launch { val n = vm.affected(); if (n > 0) impact = n else vm.save(onClose) } },
        onClose = onClose,
    ) { padding ->
        val selected = draft ?: return@EditorScaffold
        if (config == null) return@EditorScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                if (vm.subjectId != null) "These items are added whenever this subject is on the timetable."
                else "These general items appear in “For the day” every ${DateSelection.dayName(vm.day!!)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.NavyMuted,
            )
            if (config.items.isEmpty()) Banner("Your item library is empty.", modifier = Modifier.padding(top = 12.dp))
            ItemCategory.entries.forEach { cat ->
                val items = config.items.filter { it.category == cat }
                if (items.isNotEmpty()) {
                    SectionHeader(cat.label)
                    items.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .toggleable(item.id in selected, role = Role.Checkbox) { vm.toggle(item.id) }
                                .ruled(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(item.iconKey, null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                val sub = listOfNotNull(item.note, linkSummary(config, item.id)).joinToString(" · ")
                                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
                            }
                            Checkbox(checked = item.id in selected, onCheckedChange = null)
                        }
                    }
                }
            }
            TextButton(onClick = onAddItem, modifier = Modifier.padding(top = 8.dp)) { Text("Add a new item to the library") }
        }
    }
    ImpactDialog(impact, onConfirm = { impact = null; vm.save(onClose) }, onDismiss = { impact = null })
}

/** e.g. "Mathematics, Science · Tue" — shows where an item is used so identical names can be told apart. */
fun linkSummary(config: com.classprep.junior.domain.model.WeekConfig, itemId: Long): String? {
    val subjects = config.subjectItems.filterValues { itemId in it }.keys.mapNotNull { config.subject(it)?.name }
    val days = config.weekdayItems.filterValues { itemId in it }.keys.sorted().map { DateSelection.shortDayName(it) }
    val parts = listOfNotNull(subjects.takeIf { it.isNotEmpty() }?.joinToString(", "), days.takeIf { it.isNotEmpty() }?.joinToString(", "))
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
fun TaskEditorScreen(vm: TaskEditorViewModel, onClose: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var picker by rememberSaveable { mutableStateOf(false) }
    var dateMenu by remember { mutableStateOf(false) }
    var confirmedWarn by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val d = draft
    EditorScaffold(
        title = if (vm.taskId == 0L) "Add homework or task" else "Edit task",
        dirty = vm.isDirty || (vm.taskId == 0L && d != null && d.name.isNotBlank()),
        saveEnabled = d != null && d.name.isNotBlank(),
        saving = saving,
        onSave = { scope.launch { if (vm.isConfirmed()) confirmedWarn = 1 else vm.save(onClose) } },
        onClose = onClose,
    ) { padding ->
        if (d == null) return@EditorScaffold
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                OutlinedButton(onClick = { dateMenu = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Date: ${DateSelection.longDate(d.date)}") }
                DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                    (0L..Limits.PLANNING_DAYS_AHEAD).map { vm.today.plusDays(it) }.forEach { date ->
                        DropdownMenuItem(text = { Text(DateSelection.longDate(date)) }, onClick = { dateMenu = false; vm.update { it.copy(date = date) } })
                    }
                }
            }
            OutlinedTextField(
                value = d.name,
                onValueChange = { v -> if (v.length <= Limits.ITEM_NAME_MAX) vm.update { it.copy(name = v) } },
                label = { Text("What to prepare") },
                supportingText = { Text("${d.name.trim().length}/${Limits.ITEM_NAME_MAX}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = d.note,
                onValueChange = { v -> if (v.length <= Limits.NOTE_MAX) vm.update { it.copy(note = v) } },
                label = { Text("Note (optional)") },
                supportingText = { Text("${d.note.length}/${Limits.NOTE_MAX}") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Category", style = MaterialTheme.typography.titleMedium)
            Column {
                ItemCategory.entries.forEach { cat ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(d.category == cat, role = Role.RadioButton) { vm.update { it.copy(category = cat) } }
                            .background(if (d.category == cat) Paper.Tab else Paper.Background),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(selected = d.category == cat, onClick = null)
                        Text(cat.label)
                    }
                }
            }
            IconField("Choose icon", d.iconKey) { picker = true }
            Text("Ticking a task records that it is packed and ready — it does not check the work.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
            error?.let { Banner(it, background = Paper.WarningLight) }
        }
    }
    if (picker && d != null) {
        IconPickerDialog("Task icon", IconKeys.itemOptions, d.iconKey, onPick = { k -> vm.update { it.copy(iconKey = k) }; picker = false }, onDismiss = { picker = false })
    }
    ImpactDialog(confirmedWarn, onConfirm = { confirmedWarn = null; vm.save(onClose) }, onDismiss = { confirmedWarn = null })
}
