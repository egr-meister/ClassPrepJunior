package com.classprep.junior.ui.common

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.classprep.junior.R
import com.classprep.junior.domain.model.IconKeys
import com.classprep.junior.ui.theme.AppIcons
import com.classprep.junior.ui.theme.Paper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun AppIcon(key: String, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = Paper.Navy, size: Dp = 24.dp) {
    Icon(painterResource(AppIcons.forKey(key)), contentDescription, modifier.size(size), tint = tint)
}

@Composable
fun UiIcon(@DrawableRes res: Int, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = Paper.Navy) {
    Icon(painterResource(res), contentDescription, modifier.size(24.dp), tint = tint)
}

/** Top bar that respects status-bar and cutout insets. */
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = Paper.Background) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) { UiIcon(R.drawable.ic_ui_back, backLabel) }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            actions()
        }
    }
}

@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Paper.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title, onBack, actions = actions) },
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Paper.Navy,
        modifier = modifier
            .padding(top = 16.dp, bottom = 6.dp)
            .semantics { heading() },
    )
}

/** A notebook sheet: warm paper, a soft border and a red margin line. Readable at any font scale. */
@Composable
fun NotebookSheet(modifier: Modifier = Modifier, marginLine: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = Paper.Sheet,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Paper.Rule),
    ) {
        Column(
            Modifier.drawBehind {
                if (marginLine) {
                    val x = 44.dp.toPx()
                    drawLine(Paper.Margin, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
                }
            },
            content = content,
        )
    }
}

/** Draws a ruled line under a row. */
fun Modifier.ruled(): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(Paper.Rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}

/**
 * Ready checkbox: a 48dp target with a box and a check mark. State is also conveyed by the check shape and
 * by text ("Ready"/"Not ready"), never by colour alone.
 */
@Composable
fun ReadyCheck(checked: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (checked) Paper.GreenLight else Paper.Sheet)
                .border(2.dp, if (checked) Paper.Green else Paper.NavyMuted, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) UiIcon(R.drawable.ic_ui_check, null, tint = Paper.Green)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper.Sheet,
        title = { Text(title) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) Paper.Error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/**
 * Configuration form with explicit Save and Cancel. Back or Cancel with unsaved edits asks before discarding.
 */
@Composable
fun EditorScaffold(
    title: String,
    dirty: Boolean,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    saving: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    var askDiscard by rememberSaveable { mutableStateOf(false) }
    val requestClose = { if (dirty) askDiscard = true else onClose() }
    BackHandler(enabled = dirty) { askDiscard = true }
    ScreenScaffold(
        title = title,
        onBack = requestClose,
        bottomBar = {
            Surface(color = Paper.Deep) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = requestClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                    Button(onClick = onSave, enabled = saveEnabled && !saving, modifier = Modifier.heightIn(min = 48.dp)) {
                        if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save")
                    }
                }
            }
        },
        content = content,
    )
    if (askDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your unsaved edits will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            destructive = true,
            onConfirm = { askDiscard = false; onClose() },
            onDismiss = { askDiscard = false },
        )
    }
}

@Composable
fun IconPickerDialog(
    title: String,
    options: List<IconKeys.Option>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Paper.Sheet,
        title = { Text(title) },
        text = {
            LazyVerticalGrid(columns = GridCells.Adaptive(76.dp), modifier = Modifier.heightIn(max = 360.dp)) {
                items(options, key = { it.key }) { opt ->
                    val isSel = opt.key == selected
                    Column(
                        Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) Paper.Tab else Color.Transparent)
                            .border(if (isSel) 2.dp else 0.dp, if (isSel) Paper.TabSelected else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable(role = Role.RadioButton) { onPick(opt.key) }
                            .sizeIn(minHeight = 72.dp)
                            .padding(6.dp)
                            .semantics { stateDescription = if (isSel) "Selected" else "Not selected" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(opt.key, null, size = 28.dp)
                        Text(opt.label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** Icon + label button that opens the icon picker. */
@Composable
fun IconField(label: String, key: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        AppIcon(key, null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/**
 * "Parents" control: press and hold for three seconds (accidental-entry safeguard, not security).
 * A tap opens an accessible alternative dialog; screen readers also get a custom action.
 */
@Composable
fun ParentsHoldButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var showAlt by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                stateDescription = "Press and hold for three seconds, or double-tap for options"
                customActions = listOf(CustomAccessibilityAction("Open parent area") { showAlt = true; true })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    job = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(3000, easing = LinearEasing))
                        progress.snapTo(0f)
                        onOpen()
                    }
                    val up = waitForUpOrCancellation()
                    val wasRunning = job?.isActive == true
                    job?.cancel()
                    scope.launch { progress.snapTo(0f) }
                    if (up != null && wasRunning && progress.value < 0.1f) showAlt = true
                }
            },
        shape = CircleShape,
        color = Paper.Tab,
        border = BorderStroke(1.dp, Paper.Rule),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 3.dp,
                    color = Paper.TabSelected,
                    trackColor = Color.Transparent,
                )
                UiIcon(R.drawable.ic_ui_lock, null, Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Parents", style = MaterialTheme.typography.labelLarge, color = Paper.Navy)
        }
    }
    if (showAlt) {
        AlertDialog(
            onDismissRequest = { showAlt = false },
            containerColor = Paper.Sheet,
            title = { Text("Parent area") },
            text = {
                Text(
                    "Press and hold the Parents button for three seconds to open it, or use the button below. " +
                        "This only prevents accidental taps; it is not a password.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(onClick = { showAlt = false; onOpen() }, colors = ButtonDefaults.buttonColors()) { Text("Open parent area") }
            },
            dismissButton = { TextButton(onClick = { showAlt = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun Banner(text: String, modifier: Modifier = Modifier, background: Color = Paper.Highlight, action: (@Composable () -> Unit)? = null) {
    Surface(modifier = modifier.fillMaxWidth(), color = background, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Paper.Navy)
            action?.invoke()
        }
    }
}
