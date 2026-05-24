package ca.sb.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.sb.gymlogger.data.CardioPhase
import ca.sb.gymlogger.ui.components.AppCard
import ca.sb.gymlogger.ui.components.AppDatePickerDialog
import ca.sb.gymlogger.ui.components.DateChip
import ca.sb.gymlogger.ui.components.DraftRestoredBanner
import ca.sb.gymlogger.ui.components.PrimaryActionButton
import ca.sb.gymlogger.ui.viewmodel.CardioType
import ca.sb.gymlogger.ui.viewmodel.CardioViewModel
import ca.sb.gymlogger.ui.viewmodel.workoutViewModel

/**
 * A suggestion chip for the phase editor. Tapping it fills the label and,
 * optionally, prefills speed/incline if a preset is defined.
 */
private data class PhasePreset(
    val label: String,
    val speedStart: String? = null,
    val speedEnd: String? = null,
    val inclineStart: String? = null,
    val inclineEnd: String? = null
)

// Common phase suggestions. Some have preset numbers; others just set the label.
private val phasePresets = listOf(
    PhasePreset("Warm-up", speedStart = "3.5", speedEnd = "3.5", inclineStart = "0", inclineEnd = "0"),
    PhasePreset("Steady walk"),
    PhasePreset("Incline walk"),
    PhasePreset("Run"),
    PhasePreset("Sprint"),
    PhasePreset("Recovery walk"),
    PhasePreset("Cool-down", speedStart = "2.9", speedEnd = "2.5", inclineStart = "0", inclineEnd = "0")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioScreen(
    onBack: () -> Unit = {},
    initialDateKey: Int? = null,
    editingSessionId: Int? = null
) {
    val viewModel: CardioViewModel = workoutViewModel { CardioViewModel(it) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    // Phase editor state: -1 = closed, -2 = adding new, >=0 = editing index
    var phaseEditorIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(editingSessionId) {
        if (editingSessionId != null && editingSessionId > 0) {
            viewModel.loadFromSession(editingSessionId)
        }
    }

    LaunchedEffect(initialDateKey) {
        if (editingSessionId == null && initialDateKey != null) {
            viewModel.selectedDateKey = initialDateKey
        }
    }

    // Restore from draft on plain entry (no editing, no date prefill).
    LaunchedEffect(Unit) {
        if (editingSessionId == null && initialDateKey == null) {
            viewModel.loadDraftIfAny()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CARDIO",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    PrimaryActionButton(
                        text = if (viewModel.isSaving) "Saving" else "Finish",
                        onClick = { viewModel.save(onDone = onBack) },
                        enabled = viewModel.isValid && !viewModel.isSaving,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.wasRestoredFromDraft) {
                DraftRestoredBanner(
                    onDiscardRequest = { showDiscardConfirm = true },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            DateChip(
                dateKey = viewModel.selectedDateKey,
                onClick = { showDatePicker = true },
                modifier = Modifier.padding(top = 4.dp)
            )

            // Type chip row
            CardioTypeChipRow(
                selected = viewModel.cardioType,
                onSelect = { viewModel.cardioType = it }
            )

            // Session totals — always shown
            SessionTotalsCard(viewModel = viewModel)

            // Phases (treadmill only) OR notes form (other types)
            if (viewModel.cardioType == CardioType.TREADMILL) {
                PhasesSection(
                    phases = viewModel.phases,
                    editingIndex = phaseEditorIndex,
                    onAddPhase = { phaseEditorIndex = -2 },
                    onEditPhase = { index -> phaseEditorIndex = index },
                    onCloseEditor = { phaseEditorIndex = -1 },
                    onSaveNew = { phase ->
                        viewModel.addPhase(phase)
                        phaseEditorIndex = -1
                    },
                    onSaveEdit = { index, phase ->
                        viewModel.editPhase(index, phase)
                        phaseEditorIndex = -1
                    },
                    onDeletePhase = { index ->
                        viewModel.removePhase(index)
                        phaseEditorIndex = -1
                    }
                )

                // Notes also available on treadmill
                NotesCard(viewModel = viewModel)
            } else {
                NotesCard(viewModel = viewModel)
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDateKey = viewModel.selectedDateKey,
            onConfirm = {
                viewModel.selectedDateKey = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard in-progress workout?") },
            text = { Text("This will clear everything you've logged so far. The action can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardDraft()
                    showDiscardConfirm = false
                }) {
                    Text("DISCARD", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    viewModel.saveError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Save failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Collapsible type chip row. By default shows only the currently-selected type plus
 * a small "change" link. Tapping the link reveals the full set of options. Picking
 * one collapses back to the compact form.
 *
 * Treadmill is the 90% case, so this keeps the screen clean while leaving the others
 * one tap away when needed.
 */
@Composable
private fun CardioTypeChipRow(
    selected: CardioType,
    onSelect: (CardioType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardioType.values().forEach { type ->
                TypeChip(
                    label = type.label,
                    isSelected = type == selected,
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TypeChip(
                label = selected.label,
                isSelected = true,
                onClick = { expanded = true }
            )
            Text(
                text = "Change type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { expanded = true }
            )
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .then(
                if (!isSelected)
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = fg
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionTotalsCard(viewModel: CardioViewModel) {
    // Pace only makes sense for treadmill + running.
    val showPace = viewModel.cardioType == CardioType.TREADMILL ||
        viewModel.cardioType == CardioType.RUNNING

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SESSION TOTALS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyledTextField(
                    value = viewModel.duration,
                    onValueChange = { viewModel.duration = it },
                    label = "DURATION (MIN)",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                StyledTextField(
                    value = viewModel.distance,
                    onValueChange = { viewModel.distance = it },
                    label = "DISTANCE (MI)",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
                StyledTextField(
                    value = viewModel.calories,
                    onValueChange = { viewModel.calories = it },
                    label = "CALORIES",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }

            if (showPace) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PACE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val pace = viewModel.computedPace
                    Text(
                        text = if (pace != null) "$pace / mi" else "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pace != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(viewModel: CardioViewModel) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            StyledTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.notes = it },
                label = "NOTES",
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

@Composable
private fun PhasesSection(
    phases: List<CardioPhase>,
    editingIndex: Int,
    onAddPhase: () -> Unit,
    onEditPhase: (Int) -> Unit,
    onCloseEditor: () -> Unit,
    onSaveNew: (CardioPhase) -> Unit,
    onSaveEdit: (Int, CardioPhase) -> Unit,
    onDeletePhase: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "PHASES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        phases.forEachIndexed { index, phase ->
            if (editingIndex == index) {
                // Show editor in place
                PhaseEditor(
                    initial = phase,
                    onSave = { onSaveEdit(index, it) },
                    onCancel = onCloseEditor,
                    onDelete = { onDeletePhase(index) },
                    isNew = false
                )
            } else {
                PhaseRow(
                    index = index,
                    phase = phase,
                    onTap = { onEditPhase(index) }
                )
            }
        }

        // New phase editor (when adding)
        if (editingIndex == -2) {
            PhaseEditor(
                initial = CardioPhase(),
                onSave = onSaveNew,
                onCancel = onCloseEditor,
                onDelete = null,
                isNew = true
            )
        }

        // Add button (hidden while editor is open)
        if (editingIndex == -1) {
            FilledTonalButton(
                onClick = onAddPhase,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD PHASE", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PhaseRow(
    index: Int,
    phase: CardioPhase,
    onTap: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onTap) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PHASE ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = phase.label.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = phaseSummary(phase),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Build a short description like "10 min · 3.5→3.0 mph · 0→7.5% incline" */
private fun phaseSummary(phase: CardioPhase): String {
    val parts = mutableListOf<String>()
    if (phase.durationMin.isNotBlank()) parts.add("${phase.durationMin} min")
    val speed = rangeOrSingle(phase.speedStart, phase.speedEnd)
    if (speed != null) parts.add("$speed mph")
    val incline = rangeOrSingle(phase.inclineStart, phase.inclineEnd)
    if (incline != null) parts.add("$incline% incline")
    return parts.joinToString(" · ").ifBlank { "No details" }
}

/**
 * Format a start/end pair:
 * - both blank → null
 * - both same → "X"
 * - only start → "X"
 * - only end → "X"
 * - both differ → "X → Y"
 */
internal fun rangeOrSingle(start: String, end: String): String? {
    val s = start.trim()
    val e = end.trim()
    return when {
        s.isBlank() && e.isBlank() -> null
        s.isBlank() -> e
        e.isBlank() -> s
        s == e -> s
        else -> "$s→$e"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseEditor(
    initial: CardioPhase,
    onSave: (CardioPhase) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?,
    isNew: Boolean
) {
    var label by remember { mutableStateOf(initial.label) }
    var durationMin by remember { mutableStateOf(initial.durationMin) }
    var speedStart by remember { mutableStateOf(initial.speedStart) }
    var speedEnd by remember { mutableStateOf(initial.speedEnd) }
    var inclineStart by remember { mutableStateOf(initial.inclineStart) }
    var inclineEnd by remember { mutableStateOf(initial.inclineEnd) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isNew) "NEW PHASE" else "EDIT PHASE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Label field
            StyledTextField(
                value = label,
                onValueChange = { label = it },
                label = "LABEL",
                modifier = Modifier.fillMaxWidth()
            )

            // Suggestion chips. Tapping one fills the label, plus any speed/incline preset.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                phasePresets.forEach { preset ->
                    SuggestionChip(
                        text = preset.label,
                        onClick = {
                            label = preset.label
                            preset.speedStart?.let { speedStart = it }
                            preset.speedEnd?.let { speedEnd = it }
                            preset.inclineStart?.let { inclineStart = it }
                            preset.inclineEnd?.let { inclineEnd = it }
                        }
                    )
                }
            }

            // Duration
            StyledTextField(
                value = durationMin,
                onValueChange = { durationMin = it },
                label = "DURATION (MIN)",
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Number
            )

            // Speed start/end
            Text(
                text = "SPEED (MPH)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyledTextField(
                    value = speedStart,
                    onValueChange = { speedStart = it },
                    label = "START",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
                StyledTextField(
                    value = speedEnd,
                    onValueChange = { speedEnd = it },
                    label = "LOW (IF DIFFERENT)",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
            }

            // Incline start/peak
            Text(
                text = "INCLINE (%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyledTextField(
                    value = inclineStart,
                    onValueChange = { inclineStart = it },
                    label = "START",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
                StyledTextField(
                    value = inclineEnd,
                    onValueChange = { inclineEnd = it },
                    label = "PEAK (IF DIFFERENT)",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal
                )
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete phase",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) {
                    Text(
                        "CANCEL",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                PrimaryActionButton(
                    text = "Save",
                    onClick = {
                        onSave(
                            CardioPhase(
                                label = label.trim(),
                                durationMin = durationMin.trim(),
                                speedStart = speedStart.trim(),
                                speedEnd = speedEnd.trim(),
                                inclineStart = inclineStart.trim(),
                                inclineEnd = inclineEnd.trim()
                            )
                        )
                    },
                    enabled = label.isNotBlank() || durationMin.isNotBlank()
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
