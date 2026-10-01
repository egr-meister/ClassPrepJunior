package com.classprep.junior.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classprep.junior.R
import com.classprep.junior.data.repository.PlannerRepository
import com.classprep.junior.domain.model.HistoryEntry
import com.classprep.junior.domain.model.HistorySnapshot
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.ui.common.AppIcon
import com.classprep.junior.ui.common.ConfirmDialog
import com.classprep.junior.ui.common.NotebookSheet
import com.classprep.junior.ui.common.ScreenScaffold
import com.classprep.junior.ui.common.SectionHeader
import com.classprep.junior.ui.common.UiIcon
import com.classprep.junior.ui.common.ruled
import com.classprep.junior.ui.theme.Paper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryViewModel(private val repo: PlannerRepository) : ViewModel() {
    val entries: StateFlow<List<HistoryEntry>?> = repo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _detail = MutableStateFlow<HistorySnapshot?>(null)
    val detail: StateFlow<HistorySnapshot?> = _detail

    fun load(id: Long) = viewModelScope.launch { _detail.value = repo.historyDetail(id) }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteHistory(id) }
    fun clear() = viewModelScope.launch { repo.clearHistory() }
}

private val timeFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)
fun formatInstant(i: Instant): String = timeFormat.format(i.atZone(ZoneId.systemDefault()))

@Composable
fun HistoryListScreen(vm: HistoryViewModel, canDelete: Boolean, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    ScreenScaffold(
        title = "Preparation history",
        onBack = onBack,
        actions = {
            if (canDelete && !entries.isNullOrEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
        },
    ) { padding ->
        val list = entries ?: return@ScreenScaffold
        if (list.isEmpty()) {
            Text(
                "No confirmed days yet. A day appears here after its checklist is confirmed ready.",
                modifier = Modifier.padding(padding).padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            return@ScreenScaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Text(
                    "The latest ${com.classprep.junior.domain.planning.Limits.HISTORY_KEEP} confirmations are kept. Entries are read-only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Paper.NavyMuted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(list, key = { it.id }) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onOpen(e.id) }
                        .heightIn(min = 64.dp)
                        .ruled(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(DateSelection.longDate(e.targetDate), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Confirmed ${formatInstant(e.confirmedAt)} · ${e.itemCount} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = Paper.NavyMuted,
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = { confirmDeleteId = e.id }) { UiIcon(R.drawable.ic_ui_delete, "Delete entry") }
                    }
                }
            }
        }
    }
    confirmDeleteId?.let { id ->
        ConfirmDialog(
            "Delete this entry?", "This history entry will be removed.", "Delete",
            onConfirm = { vm.delete(id); confirmDeleteId = null }, onDismiss = { confirmDeleteId = null }, destructive = true,
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            "Clear all history?", "All confirmed-day records will be removed. Plans and settings stay.", "Clear history",
            onConfirm = { vm.clear(); confirmClear = false }, onDismiss = { confirmClear = false }, destructive = true,
        )
    }
}

@Composable
fun HistoryDetailScreen(vm: HistoryViewModel, id: Long, onBack: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(id) { vm.load(id) }
    val d by vm.detail.collectAsStateWithLifecycle()
    ScreenScaffold(title = "History entry", onBack = onBack) { padding ->
        val s = d ?: return@ScreenScaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(DateSelection.longDate(s.targetDate), style = MaterialTheme.typography.headlineSmall)
            Text("Confirmed ${formatInstant(s.confirmedAt)} · ${s.itemCount} items prepared", style = MaterialTheme.typography.bodyMedium, color = Paper.NavyMuted)
            if (s.lessons.isNotEmpty()) {
                SectionHeader("Lessons")
                NotebookSheet(Modifier.fillMaxWidth()) {
                    s.lessons.forEach { l ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).ruled().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${l.position}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.titleMedium)
                            AppIcon(l.iconKey, null)
                            Spacer(Modifier.width(10.dp))
                            Text(l.subjectName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            ItemCategory.entries.forEach { cat ->
                val items = s.items.filter { it.category == cat }
                if (items.isNotEmpty()) {
                    SectionHeader(cat.label)
                    NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                        items.forEach { item ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).ruled().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(item.iconKey, null)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(item.subjectLabels, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
                                    item.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
