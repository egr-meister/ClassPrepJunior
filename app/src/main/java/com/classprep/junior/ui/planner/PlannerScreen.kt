package com.classprep.junior.ui.planner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classprep.junior.R
import com.classprep.junior.domain.model.PlanStatus
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.domain.planning.Limits
import com.classprep.junior.ui.common.AppIcon
import com.classprep.junior.ui.common.AppTopBar
import com.classprep.junior.ui.common.Banner
import com.classprep.junior.ui.common.ConfirmDialog
import com.classprep.junior.ui.common.NotebookSheet
import com.classprep.junior.ui.common.ParentsHoldButton
import com.classprep.junior.ui.common.ReadyCheck
import com.classprep.junior.ui.common.UiIcon
import com.classprep.junior.ui.common.ruled
import com.classprep.junior.ui.theme.LessonNumberStyle
import com.classprep.junior.ui.theme.Paper
import java.time.LocalDate

data class PlannerActions(
    val onReview: (LocalDate) -> Unit,
    val onHistory: () -> Unit,
    val onParents: () -> Unit,
    val onExitExample: () -> Unit,
)

@Composable
fun PlannerScreen(vm: PlannerViewModel, actions: PlannerActions, openDate: LocalDate?, onOpenDateConsumed: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val taskError by vm.taskError.collectAsState()
    LaunchedEffect(openDate) {
        if (openDate != null) {
            vm.openDate(openDate)
            onOpenDateConsumed()
        }
    }
    // Back from a filtered checklist restores the full list; otherwise Back behaves normally.
    BackHandler(enabled = state.filter != null) { vm.setFilter(null) }
    if (state.isExample) BackHandler { actions.onExitExample() }

    var showAddTask by rememberSaveable { mutableStateOf(false) }
    var confirmParents by rememberSaveable { mutableStateOf(false) }
    var confirmUndo by rememberSaveable { mutableStateOf(false) }

    androidx.compose.material3.Scaffold(
        containerColor = Paper.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppTopBar(
                title = if (state.isExample) "Example week" else "School Planner",
                onBack = if (state.isExample) actions.onExitExample else null,
                backLabel = "Close example",
            ) {
                if (!state.isExample) {
                    IconButton(onClick = actions.onHistory) { UiIcon(R.drawable.ic_ui_history, "Preparation history") }
                    ParentsHoldButton(onOpen = actions.onParents, modifier = Modifier.padding(end = 8.dp))
                }
            }
        },
        bottomBar = {
            if (state.total > 0 && state.editable) {
                PreparationStrip(state.ready, state.total, state.status, onReview = { actions.onReview(state.date) })
            }
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 600.dp
            val header: @Composable () -> Unit = {
                PlannerHeader(state, onSelect = vm::selectDate, onPrev = vm::previousWeek, onNext = vm::nextWeek)
                StatusBanners(state, onUndo = { confirmUndo = true })
            }
            val timetable: @Composable () -> Unit = {
                if (!state.isSchoolDay) {
                    NoSchoolCard(state, onPrepareNext = { d -> vm.selectDate(d) }, onParentSetup = { confirmParents = true })
                } else {
                    LessonsSheet(state, onSelect = vm::setFilter)
                }
            }
            val checklist: @Composable () -> Unit = {
                ChecklistSheet(
                    state = state,
                    onToggle = vm::toggle,
                    onAllItems = { vm.setFilter(null) },
                    onAddTask = { showAddTask = true },
                    onParentSetup = { confirmParents = true },
                    onHistory = actions.onHistory,
                )
            }
            if (wide) {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(horizontal = 24.dp)) { header() }
                    Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(0.42f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) { timetable() }
                        Column(Modifier.weight(0.58f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) { checklist() }
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                ) {
                    header()
                    timetable()
                    Spacer(Modifier.height(16.dp))
                    checklist()
                }
            }
        }
    }

    if (showAddTask) {
        AddTaskDialog(
            dateLabel = state.longDate,
            error = taskError,
            onSave = { name, note -> vm.addTask(name, note) { showAddTask = false } },
            onDismiss = { showAddTask = false; vm.clearTaskError() },
        )
    }
    if (confirmParents) {
        ConfirmDialog(
            title = "Parent setup",
            text = "Ask a parent to add lessons and preparation items in the parent area.",
            confirmLabel = "Open parent area",
            onConfirm = { confirmParents = false; actions.onParents() },
            onDismiss = { confirmParents = false },
        )
    }
    if (confirmUndo) {
        ConfirmDialog(
            title = "Undo ready?",
            text = "The plan will need to be reviewed and confirmed again. Your ticks stay as they are.",
            confirmLabel = "Undo ready",
            onConfirm = { confirmUndo = false; vm.undoReady() },
            onDismiss = { confirmUndo = false },
        )
    }
}

@Composable
private fun PlannerHeader(state: PlannerUiState, onSelect: (LocalDate) -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    if (state.isExample) {
        Banner(
            "Example only — sample lessons and items. Nothing here is saved, used for reminders or added to history.",
            modifier = Modifier.padding(top = 4.dp),
            background = Paper.Tab,
        )
    }
    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(state.header, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(state.longDate, style = MaterialTheme.typography.bodyLarge, color = Paper.NavyMuted)
        }
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.Image(
            painterResource(R.drawable.ill_pencil),
            contentDescription = null,
            modifier = Modifier.width(72.dp).height(18.dp),
        )
    }
    WeekStrip(state, onSelect, onPrev, onNext)
}

@Composable
private fun WeekStrip(state: PlannerUiState, onSelect: (LocalDate) -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = state.canPrevWeek) {
            UiIcon(R.drawable.ic_ui_chevron_left, "Previous week", tint = if (state.canPrevWeek) Paper.Navy else Paper.Rule)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.week.forEach { cell ->
                val bg = when {
                    cell.selected -> Paper.TabSelected
                    cell.selectable -> Paper.Tab
                    else -> Paper.Deep
                }
                val fg = when {
                    cell.selected -> Color.White
                    cell.selectable -> Paper.Navy
                    else -> Paper.NavyMuted
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(bg)
                        .clickable(enabled = cell.selectable, role = Role.Tab) { onSelect(cell.date) }
                        .heightIn(min = 56.dp)
                        .padding(vertical = 6.dp)
                        .semantics(mergeDescendants = true) {
                            selected = cell.selected
                            contentDescription = DateSelection.longDate(cell.date) +
                                (if (cell.isSchoolDay) ", school day" else ", no school") +
                                (if (cell.isToday) ", today" else "") +
                                (if (!cell.selectable) ", not available" else "")
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(cell.shortName, style = MaterialTheme.typography.labelMedium, color = fg)
                    Text(
                        cell.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = fg,
                        fontWeight = if (cell.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                    )
                    // School-day marker: a dot (shape, not only colour).
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (cell.isSchoolDay) fg else Color.Transparent),
                    )
                }
            }
        }
        IconButton(onClick = onNext, enabled = state.canNextWeek) {
            UiIcon(R.drawable.ic_ui_chevron_right, "Next week", tint = if (state.canNextWeek) Paper.Navy else Paper.Rule)
        }
    }
}

@Composable
private fun StatusBanners(state: PlannerUiState, onUndo: () -> Unit) {
    when (state.status) {
        PlanStatus.CONFIRMED -> Banner(
            "✓ Confirmed ready. You marked everything on this list as prepared.",
            modifier = Modifier.padding(bottom = 8.dp),
            background = Paper.GreenLight,
        ) {
            if (state.editable) TextButton(onClick = onUndo) { Text("Undo ready") }
        }
        PlanStatus.NEEDS_REVIEW -> Banner(
            "Needs review — something changed after this day was confirmed. Check the list and confirm again.",
            modifier = Modifier.padding(bottom = 8.dp),
            background = Paper.WarningLight,
        )
        PlanStatus.OPEN -> Unit
    }
    if (state.overLimit) {
        Banner(
            "For parents: this day has ${state.total} checklist entries, more than the limit of ${Limits.MAX_CHECKLIST_ENTRIES}. " +
                "All items are still shown. Please shorten the subject, weekday or task lists in the parent area.",
            modifier = Modifier.padding(bottom = 8.dp),
            background = Paper.WarningLight,
        )
    }
}

@Composable
private fun NoSchoolCard(state: PlannerUiState, onPrepareNext: (LocalDate) -> Unit, onParentSetup: () -> Unit) {
    NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (state.isTomorrow) "No school planned for tomorrow." else "No school planned for ${DateSelection.dayName(state.date.dayOfWeek)}.",
                style = MaterialTheme.typography.titleMedium,
            )
            val next = state.nextSchoolDay
            when {
                !state.hasAnySchoolDay -> {
                    Text("No school days are set up yet.", style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = onParentSetup, modifier = Modifier.heightIn(min = 48.dp)) { Text("Parent setup") }
                }
                next != null -> Button(onClick = { onPrepareNext(next) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Column {
                        Text("Prepare next school day")
                        Text(DateSelection.longDate(next), style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> Text("No school day in the next two weeks.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun LessonsSheet(state: PlannerUiState, onSelect: (Long?) -> Unit) {
    NotebookSheet(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 56.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Lessons", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            if (state.filter != null) {
                TextButton(onClick = { onSelect(null) }) { Text("All items") }
            }
        }
        if (state.lessons.isEmpty()) {
            Text(
                "No lessons added for this day.",
                modifier = Modifier.padding(start = 56.dp, end = 12.dp, bottom = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.NavyMuted,
            )
        }
        state.lessons.forEach { lesson ->
            val isSel = state.filter?.position == lesson.position && state.filter.subjectId == lesson.subjectId
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isSel) Paper.Highlight else Color.Transparent)
                    .clickable(role = Role.Button) { onSelect(if (state.filter?.subjectId == lesson.subjectId) null else lesson.subjectId) }
                    .heightIn(min = 56.dp)
                    .ruled()
                    .padding(end = 12.dp)
                    .semantics(mergeDescendants = true) {
                        stateDescription = if (isSel) "Showing its items" else "Tap to show its items"
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    lesson.position.toString(),
                    style = LessonNumberStyle,
                    color = Paper.NavyMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(44.dp),
                )
                Spacer(Modifier.width(12.dp))
                AppIcon(lesson.iconKey, null)
                Spacer(Modifier.width(10.dp))
                Text(lesson.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = Paper.Highlight, border = BorderStroke(1.dp, Paper.HighlightStrong)) {
                    Text(
                        if (lesson.itemCount == 1) "1 item" else "${lesson.itemCount} items",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChecklistSheet(
    state: PlannerUiState,
    onToggle: (com.classprep.junior.domain.model.ItemKey, Boolean) -> Unit,
    onAllItems: () -> Unit,
    onAddTask: () -> Unit,
    onParentSetup: () -> Unit,
    onHistory: () -> Unit,
) {
    NotebookSheet(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 56.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Preparation list", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        }
        state.filter?.let { f ->
            Row(Modifier.padding(start = 56.dp, end = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = true, onClick = onAllItems, label = { Text("${f.name} only") })
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onAllItems) { Text("All items") }
            }
        }
        if (state.total == 0 && state.isSchoolDay) {
            Column(Modifier.padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No preparation items added", style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(onClick = onParentSetup, modifier = Modifier.heightIn(min = 48.dp)) { Text("Parent setup") }
            }
        } else if (state.total == 0) {
            Text(
                "Nothing to prepare for this day.",
                modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.NavyMuted,
            )
        } else if (state.sections.isEmpty()) {
            Text(
                "No items are linked to ${state.filter?.name ?: "this lesson"}.",
                modifier = Modifier.padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.NavyMuted,
            )
        }
        state.sections.forEach { section ->
            Text(
                section.title,
                style = MaterialTheme.typography.titleSmall,
                color = Paper.NavyMuted,
                modifier = Modifier
                    .padding(start = 56.dp, end = 12.dp, top = 14.dp, bottom = 2.dp)
                    .semantics { heading() },
            )
            section.rows.forEach { row -> ChecklistRowView(row, enabled = state.editable, onToggle = onToggle) }
        }
        if (state.editable) {
            OutlinedButton(
                onClick = onAddTask,
                modifier = Modifier.padding(start = 56.dp, top = 12.dp, bottom = 14.dp).heightIn(min = 48.dp),
            ) {
                UiIcon(R.drawable.ic_ui_add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add homework or task")
            }
        } else {
            TextButton(onClick = onHistory, modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)) {
                Text("Earlier days are in History (read-only)")
            }
        }
    }
}

@Composable
private fun ChecklistRowView(row: ChecklistRow, enabled: Boolean, onToggle: (com.classprep.junior.domain.model.ItemKey, Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (row.ready) Paper.GreenLight.copy(alpha = 0.45f) else Color.Transparent)
            .toggleable(value = row.ready, enabled = enabled, role = Role.Checkbox) { onToggle(row.key, it) }
            .heightIn(min = 56.dp)
            .ruled()
            .padding(end = 8.dp)
            .semantics(mergeDescendants = true) { stateDescription = if (row.ready) "Ready" else "Not ready" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) { AppIcon(row.iconKey, null) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge)
            Text(row.relatedLabel, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
            row.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted) }
        }
        ReadyCheck(row.ready)
    }
}

@Composable
private fun PreparationStrip(ready: Int, total: Int, status: PlanStatus, onReview: () -> Unit) {
    Surface(color = Paper.Deep, border = BorderStroke(1.dp, Paper.Rule)) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text("$ready of $total ready", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else ready.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 12.dp).height(6.dp).clip(CircleShape),
                    color = Paper.Green,
                    trackColor = Paper.Rule,
                    drawStopIndicator = {},
                )
            }
            Button(onClick = onReview, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 120.dp)) {
                Text(if (status == PlanStatus.CONFIRMED) "View readiness" else "Review readiness")
            }
        }
    }
}

@Composable
private fun AddTaskDialog(dateLabel: String, error: String?, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper.Sheet,
        title = { Text("Add homework or task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("For $dateLabel", style = MaterialTheme.typography.bodyMedium, color = Paper.NavyMuted)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= Limits.ITEM_NAME_MAX) name = it },
                    label = { Text("What to prepare") },
                    supportingText = { Text("${name.trim().length}/${Limits.ITEM_NAME_MAX}") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= Limits.NOTE_MAX) note = it },
                    label = { Text("Note (optional)") },
                    supportingText = { Text("${note.length}/${Limits.NOTE_MAX}") },
                )
                Text(
                    "Ticking a task means it is packed and ready to bring — not that the work has been checked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Paper.NavyMuted,
                )
                error?.let { Text(it, color = Paper.Error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, note) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
