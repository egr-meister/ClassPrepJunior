package com.classprep.junior.ui.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classprep.junior.R
import com.classprep.junior.domain.model.PlanStatus
import com.classprep.junior.domain.planning.DateSelection
import com.classprep.junior.ui.common.AppIcon
import com.classprep.junior.ui.common.Banner
import com.classprep.junior.ui.common.NotebookSheet
import com.classprep.junior.ui.common.ReadyCheck
import com.classprep.junior.ui.common.ScreenScaffold
import com.classprep.junior.ui.common.ruled
import com.classprep.junior.ui.planner.ChecklistRow
import com.classprep.junior.ui.theme.Paper

@Composable
fun ReviewScreen(vm: ReviewViewModel, onBack: () -> Unit, onConfirmed: () -> Unit, onParentSetup: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val confirmed by vm.confirmed.collectAsStateWithLifecycle()
    LaunchedEffect(confirmed) { if (confirmed) onConfirmed() }

    ScreenScaffold(title = s.title, onBack = onBack) { padding ->
        if (s.loading) return@ScreenScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(s.longDate, style = MaterialTheme.typography.bodyLarge, color = Paper.NavyMuted)
            if (s.isExample) Banner("Example only — confirming here is not saved.", background = Paper.Tab)
            when {
                s.total == 0 -> {
                    Text("No preparation items added", style = MaterialTheme.typography.headlineSmall)
                    Text("There is nothing on this list yet, so it can't be marked ready.", style = MaterialTheme.typography.bodyLarge)
                    if (!s.isExample) OutlinedButton(onClick = onParentSetup, modifier = Modifier.heightIn(min = 48.dp)) { Text("Parent setup") }
                    OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to planner") }
                }
                s.remaining.isNotEmpty() -> {
                    Text(
                        "A few things still need preparing.",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text("${s.ready} of ${s.total} ready", style = MaterialTheme.typography.titleMedium)
                    NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) {
                        s.remaining.forEach { ReviewRow(it) }
                    }
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Continue preparing") }
                }
                s.status == PlanStatus.CONFIRMED -> {
                    Text("Already confirmed ready.", style = MaterialTheme.typography.headlineSmall)
                    Text("Change a tick or the plan to review it again.", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)) { Text("Back to planner") }
                }
                else -> {
                    Text(
                        "Everything on your list is ready.",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (s.lessons.isNotEmpty()) {
                        Text("Lessons: " + s.lessons.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
                    }
                    s.sections.forEach { section ->
                        Text(section.title, style = MaterialTheme.typography.titleSmall, color = Paper.NavyMuted)
                        NotebookSheet(Modifier.fillMaxWidth(), marginLine = false) { section.rows.forEach { ReviewRow(it) } }
                    }
                    Text(
                        "Ticks show what you marked as prepared. The app can't see inside your bag, and ticking homework doesn't check the answers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Paper.NavyMuted,
                    )
                    s.message?.let { Banner(it, background = Paper.WarningLight) }
                    Button(
                        onClick = vm::confirm,
                        enabled = !s.saving && !s.overLimit,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        if (s.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Confirm ready")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(row: ChecklistRow) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).ruled().padding(horizontal = 12.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(row.iconKey, null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge)
            Text(row.relatedLabel, style = MaterialTheme.typography.bodySmall, color = Paper.NavyMuted)
        }
        ReadyCheck(row.ready)
        Text(if (row.ready) "Ready" else "Not yet", style = MaterialTheme.typography.labelMedium, modifier = Modifier.widthIn(min = 56.dp))
    }
}

@Composable
fun ReadyScreen(vm: ReviewViewModel, onBackToPlanner: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    ScreenScaffold(title = "", onBack = onBackToPlanner) { padding ->
        if (s.loading) return@ScreenScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(painterResource(R.drawable.ill_ready_stamp), contentDescription = null, modifier = Modifier.width(180.dp))
            Text(
                DateSelection.readyTitle(s.date, s.today),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(s.longDate, style = MaterialTheme.typography.bodyLarge, color = Paper.NavyMuted)
            if (s.isExample) Banner("Example only — this confirmation was not saved.", background = Paper.Tab)
            if (s.lessons.isNotEmpty()) {
                NotebookSheet(Modifier.fillMaxWidth()) {
                    s.lessons.forEach {
                        Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).ruled().padding(start = 56.dp, end = 12.dp), contentAlignment = Alignment.CenterStart) {
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Text(
                if (s.total == 1) "1 item marked as prepared" else "${s.total} items marked as prepared",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onBackToPlanner, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Back to planner") }
        }
    }
}
