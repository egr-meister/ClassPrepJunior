package com.classprep.junior.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classprep.junior.AppContainer
import com.classprep.junior.R
import com.classprep.junior.data.prefs.SetupProgress
import com.classprep.junior.domain.model.IconKeys
import com.classprep.junior.domain.model.ReusableItem
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.model.WeekConfig
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.domain.planning.ItemSuggestions
import com.classprep.junior.domain.planning.Limits
import com.classprep.junior.domain.planning.Validation
import com.classprep.junior.ui.common.AppIcon
import com.classprep.junior.ui.common.Banner
import com.classprep.junior.ui.common.ConfirmDialog
import com.classprep.junior.ui.common.NotebookSheet
import com.classprep.junior.ui.common.ScreenScaffold
import com.classprep.junior.ui.common.SectionHeader
import com.classprep.junior.ui.common.UiIcon
import com.classprep.junior.ui.common.ruled
import com.classprep.junior.ui.parent.ParentViewModel
import com.classprep.junior.ui.parent.ReminderSettingsContent
import com.classprep.junior.ui.parent.linkSummary
import com.classprep.junior.ui.theme.Paper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class SetupViewModel(private val c: AppContainer) : ViewModel() {
    val progress: StateFlow<SetupProgress?> = c.settings.setup.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val config: StateFlow<WeekConfig?> = c.repository.observeConfig().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun goTo(step: Int) = viewModelScope.launch { c.settings.setSetupStep(step.coerceIn(0, STEPS - 1)) }

    fun saveSchoolDays(days: Set<DayOfWeek>, next: Int) = viewModelScope.launch {
        c.repository.saveSchoolDays(days)
        c.settings.setSetupStep(next)
    }

    fun addSubject(name: String, onDone: () -> Unit) = viewModelScope.launch {
        val cfg = config.value ?: return@launch
        Validation.subjectName(name, cfg.subjects.map { it.name })?.let { _error.value = it; return@launch }
        runCatching { c.repository.saveSubject(Subject(0, name.trim(), IconKeys.suggestSubjectIcon(name), 0)) }
            .onSuccess { _error.value = null; onDone() }
            .onFailure { _error.value = it.message }
    }

    fun deleteSubject(id: Long) = viewModelScope.launch { c.repository.deleteSubject(id) }

    fun addSuggestion(s: ItemSuggestions.Suggestion) = viewModelScope.launch {
        runCatching { c.repository.saveItem(ReusableItem(0, s.name, null, s.iconKey, s.category)) }
            .onFailure { _error.value = it.message }
    }

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        c.settings.completeSetup()
        onDone()
    }

    companion object {
        const val STEPS = 5
    }
}

@Composable
fun WelcomeScreen(progress: SetupProgress?, onParentSetup: () -> Unit, onExample: () -> Unit) {
    Surface(color = Paper.Background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            NotebookSheet(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Column(Modifier.padding(start = 60.dp, end = 20.dp, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(88.dp))
                    Text(
                        "Let’s prepare your school week.",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "A parent adds the real timetable and what to bring. Each evening, check the list and confirm you're ready.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Image(painterResource(R.drawable.ill_pencil), contentDescription = null, modifier = Modifier.width(110.dp).height(28.dp))
                }
            }
            val started = progress?.started == true
            Button(onClick = onParentSetup, modifier = Modifier.widthIn(min = 240.dp).heightIn(min = 52.dp)) {
                Text(if (started) "Continue parent setup (step ${(progress?.step ?: 0) + 1} of ${SetupViewModel.STEPS})" else "Parent setup", textAlign = TextAlign.Center)
            }
            OutlinedButton(onClick = onExample, modifier = Modifier.widthIn(min = 240.dp).heightIn(min = 52.dp)) { Text("Explore an example") }
            Text(
                "Works offline. Everything stays on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = Paper.NavyMuted,
            )
        }
    }
}

private val stepTitles = listOf("School weekdays", "Subjects", "Weekly lessons", "Preparation items", "Evening reminder")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    vm: SetupViewModel,
    parentVm: ParentViewModel,
    onExit: () -> Unit,
    onFinished: () -> Unit,
    onEditDay: (DayOfWeek) -> Unit,
    onSubjectLinks: () -> Unit,
    onWeekdayLinks: () -> Unit,
    onItemLibrary: () -> Unit,
) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val p = progress ?: return
    val config = cfg ?: return
    val step = p.step.coerceIn(0, SetupViewModel.STEPS - 1)
    LaunchedEffect(Unit) { if (!p.started) vm.goTo(0) }
    BackHandler(enabled = step > 0) { vm.goTo(step - 1) }

    var days by rememberSaveable(config.schoolDays) { mutableStateOf(config.schoolDays.map { it.value }.toSet()) }
    var confirmRemoveDays by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(
        title = "Parent setup",
        onBack = { if (step > 0) vm.goTo(step - 1) else onExit() },
        actions = { TextButton(onClick = onExit) { Text("Finish later") } },
        bottomBar = {
            Surface(color = Paper.Deep) {
                Row(
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    if (step > 0) OutlinedButton(onClick = { vm.goTo(step - 1) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
                    Button(
                        onClick = {
                            when (step) {
                                0 -> {
                                    val selected = days.map { DayOfWeek.of(it) }.toSet()
                                    val removed = config.schoolDays - selected
                                    val losesData = removed.any { d -> config.lessonsFor(d).isNotEmpty() || config.weekdayItems[d].orEmpty().isNotEmpty() }
                                    if (losesData) confirmRemoveDays = true else vm.saveSchoolDays(selected, 1)
                                }
                                SetupViewModel.STEPS - 1 -> vm.finish(onFinished)
                                else -> vm.goTo(step + 1)
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(if (step == SetupViewModel.STEPS - 1) "Finish setup" else "Save and continue") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Step ${step + 1} of ${SetupViewModel.STEPS}: ${stepTitles[step]}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            LinearProgressIndicator(progress = { (step + 1f) / SetupViewModel.STEPS }, modifier = Modifier.fillMaxWidth(), color = Paper.TabSelected, trackColor = Paper.Rule)
            Text("Progress is saved as you go. You can finish later.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
            when (step) {
                0 -> {
                    Text("Which days does your child go to school?", style = MaterialTheme.typography.bodyLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayOfWeek.entries.forEach { d ->
                            FilterChip(
                                selected = d.value in days,
                                onClick = { days = if (d.value in days) days - d.value else days + d.value },
                                label = { Text(DateSelection.dayName(d)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
                1 -> SubjectsStep(config, vm, error)
                2 -> {
                    Text("Add the lessons for each school day in order.", style = MaterialTheme.typography.bodyLarge)
                    if (config.schoolDays.isEmpty()) Banner("No school days selected. Go back to step 1.")
                    NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                        config.schoolDays.sorted().forEach { d ->
                            val lessons = config.lessonsFor(d)
                            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).ruled().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                    Text(DateSelection.dayName(d), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (lessons.isEmpty()) "No lessons yet" else lessons.joinToString(", ") { config.subject(it.subjectId)?.name ?: "?" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Paper.NavyMuted,
                                    )
                                }
                                TextButton(onClick = { onEditDay(d) }) { Text("Edit") }
                            }
                        }
                    }
                }
                3 -> {
                    Text("Add the things to bring, then choose which subjects or weekdays need them.", style = MaterialTheme.typography.bodyLarge)
                    SectionHeader("Add from suggestions")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ItemSuggestions.all.forEach { s ->
                            val already = config.items.count { it.name.equals(s.name, ignoreCase = true) }
                            AssistChip(
                                onClick = { vm.addSuggestion(s) },
                                label = { Text(if (already > 0) "${s.name} (added)" else s.name) },
                                leadingIcon = { AppIcon(s.iconKey, null) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                    error?.let { Banner(it, background = Paper.WarningLight) }
                    OutlinedButton(onClick = onItemLibrary, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open item library (${config.items.size})") }
                    if (config.items.isNotEmpty()) {
                        NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                            config.items.forEach { item ->
                                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).ruled().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(item.iconKey, null)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(linkSummary(config, item.id) ?: "Not linked yet", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
                                    }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSubjectLinks, enabled = config.subjects.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Items per subject") }
                        Button(onClick = onWeekdayLinks, enabled = config.schoolDays.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Items per weekday") }
                    }
                }
                4 -> {
                    Text("Optional. You can change this later in the parent area.", style = MaterialTheme.typography.bodyLarge)
                    ReminderSettingsContent(parentVm)
                }
            }
        }
    }
    if (confirmRemoveDays) {
        ConfirmDialog(
            title = "Remove school days?",
            text = "Days you switched off will lose their lessons and recurring weekday items.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { confirmRemoveDays = false; vm.saveSchoolDays(days.map { DayOfWeek.of(it) }.toSet(), 1) },
            onDismiss = { confirmRemoveDays = false },
        )
    }
}

@Composable
private fun SubjectsStep(config: WeekConfig, vm: SetupViewModel, error: String?) {
    var name by rememberSaveable { mutableStateOf("") }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    deleting?.let { id ->
        val subject = config.subject(id)
        val days = config.lessons.filter { it.subjectId == id }.map { it.weekday }.distinct().sorted()
        ConfirmDialog(
            title = "Remove ${subject?.name ?: "subject"}?",
            text = if (days.isEmpty()) "It is not used on any weekday yet."
            else "It is used on ${days.joinToString(", ") { DateSelection.dayName(it) }}. Those lessons will be removed.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { deleting = null; vm.deleteSubject(id) },
            onDismiss = { deleting = null },
        )
    }
    Text("Add each subject your child has. You'll put them in order on the next step.", style = MaterialTheme.typography.bodyLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= Limits.SUBJECT_NAME_MAX) name = it },
            label = { Text("Subject name") },
            singleLine = true,
            isError = error != null,
            supportingText = { Text(error ?: "${name.trim().length}/${Limits.SUBJECT_NAME_MAX}") },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = { vm.addSubject(name) { name = "" } },
            enabled = name.isNotBlank() && config.subjects.size < Limits.MAX_SUBJECTS,
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Add") }
    }
    NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
        if (config.subjects.isEmpty()) Text("No subjects yet.", Modifier.padding(16.dp), color = Paper.NavyMuted)
        config.subjects.forEach { s ->
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).ruled().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(s.iconKey, null)
                Spacer(Modifier.width(10.dp))
                Text(s.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { deleting = s.id }) { UiIcon(R.drawable.ic_ui_delete, "Remove ${s.name}") }
            }
        }
    }
    Text("Icons are chosen from the name; you can change them later in the parent area.", style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
}
