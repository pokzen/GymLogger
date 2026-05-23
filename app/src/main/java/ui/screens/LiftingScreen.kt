package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.data.ExerciseEntry
import ca.bpmproperty.gymlogger.data.SetEntry
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.AppDatePickerDialog
import ca.bpmproperty.gymlogger.ui.components.DateChip
import ca.bpmproperty.gymlogger.ui.components.DraftRestoredBanner
import ca.bpmproperty.gymlogger.ui.components.ExercisePicker
import ca.bpmproperty.gymlogger.ui.components.PrimaryActionButton
import ca.bpmproperty.gymlogger.ui.viewmodel.LiftingViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftingScreen(
    onBack: () -> Unit = {},
    initialDateKey: Int? = null,
    initialTemplateId: Int? = null,
    editingSessionId: Int? = null
) {
    val viewModel: LiftingViewModel = workoutViewModel { LiftingViewModel(it) }
    val exercises = viewModel.exercises
    var showExercisePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    // If we're editing an existing session, load it. Skip template + date prefill in that case.
    LaunchedEffect(editingSessionId) {
        if (editingSessionId != null && editingSessionId > 0) {
            viewModel.loadFromSession(editingSessionId)
        }
    }

    // Apply incoming date prefill once on first composition (only when NOT editing).
    LaunchedEffect(initialDateKey) {
        if (editingSessionId == null && initialDateKey != null) {
            viewModel.selectedDateKey = initialDateKey
        }
    }

    // Apply incoming template prefill once (only when NOT editing).
    LaunchedEffect(initialTemplateId) {
        if (editingSessionId == null && initialTemplateId != null && initialTemplateId > 0) {
            viewModel.applyTemplate(initialTemplateId)
        }
    }

    // Restore from draft when this is a plain-entry session (no template, date prefill,
    // or edit target). Lets the user pick up an interrupted workout where they left off.
    LaunchedEffect(Unit) {
        if (editingSessionId == null && initialTemplateId == null && initialDateKey == null) {
            viewModel.loadDraftIfAny()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WEIGHTS",
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
                        enabled = exercises.isNotEmpty() && !viewModel.isSaving,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExercisePicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sets Completed progress — only shown when the session has prescribed sets.
            val pct = viewModel.completionPercent
            if (pct != null) {
                SetsCompletedStrip(
                    completed = viewModel.completedPrescribedSets,
                    total = viewModel.totalPrescribedSets,
                    percent = pct,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (viewModel.wasRestoredFromDraft) {
                DraftRestoredBanner(
                    onDiscardRequest = { showDiscardConfirm = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            DateChip(
                dateKey = viewModel.selectedDateKey,
                onClick = { showDatePicker = true },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (exercises.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EMPTY WORKOUT",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add an exercise",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Render exercises grouped by muscle. A header appears whenever the
                    // muscleGroup changes from the previous exercise (or for the first item).
                    var lastGroup: String? = null
                    exercises.forEachIndexed { index, ex ->
                        val groupLabel = ex.muscleGroup.trim()
                        if (groupLabel.isNotBlank() && groupLabel != lastGroup) {
                            lastGroup = groupLabel
                            item("group-$index-$groupLabel") {
                                Text(
                                    text = groupLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        item("ex-$index") {
                            ExerciseCard(
                                exercise = exercises[index],
                                index = index,
                                prescribedReps = viewModel.prescribedReps[index] ?: emptyList(),
                                nextPrescribedReps = viewModel.nextPrescribedRepsFor(index),
                                onAddSet = { reps, weight -> viewModel.addSet(index, reps, weight) },
                                onEditSet = { setIndex, reps, weight ->
                                    viewModel.editSet(index, setIndex, reps, weight)
                                },
                                onDeleteSet = { setIndex -> viewModel.removeSet(index, setIndex) },
                                onDelete = { viewModel.removeExercise(index) }
                            )
                        }
                    }
                }
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

    if (showExercisePicker) {
        ExercisePicker(
            onSelect = { name, muscleGroup ->
                viewModel.addExercise(name, muscleGroup)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
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
 * Compact progress strip shown at the top of the lifting session. Displays
 * "SETS COMPLETED  18 / 26 · 69%" with the numbers in the brand-yellow accent.
 * Shown only when the session has at least one prescribed set.
 */
@Composable
private fun SetsCompletedStrip(
    completed: Int,
    total: Int,
    percent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SETS COMPLETED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$completed / $total",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseEntry,
    index: Int,
    prescribedReps: List<Int> = emptyList(),
    nextPrescribedReps: Int? = null,
    onAddSet: (String, String) -> Unit,
    onEditSet: (setIndex: Int, reps: String, weight: String) -> Unit,
    onDeleteSet: (setIndex: Int) -> Unit,
    onDelete: () -> Unit
) {
    // Auto-fill the reps field with the next prescribed rep value when adding a new set.
    // Track whether the user has manually typed something so we don't overwrite their input.
    var reps by remember { mutableStateOf(nextPrescribedReps?.toString() ?: "") }
    var weight by remember { mutableStateOf("") }

    // When the next prescribed rep changes (e.g., after adding a set), update the field
    // unless the user has typed a custom value.
    LaunchedEffect(nextPrescribedReps) {
        if (nextPrescribedReps != null && reps.isBlank()) {
            reps = nextPrescribedReps.toString()
        }
    }

    // Exercise is "complete" when prescribed sets are all logged.
    val isComplete = prescribedReps.isNotEmpty() && exercise.sets.size >= prescribedReps.size

    // User can also explicitly stop short of the prescription. Stopped cards collapse like
    // completed ones but don't earn a checkmark. Reversible via the + button.
    var stoppedEarly by remember { mutableStateOf(false) }
    val canStopEarly = prescribedReps.isNotEmpty() && !isComplete

    // When complete OR stopped, the chunky input row collapses. User can tap + to reopen it.
    var expandedAfterComplete by remember { mutableStateOf(false) }
    // Force-reset expansion whenever the completion state flips, in either direction.
    // This guarantees the input row collapses the instant the last prescribed set is logged,
    // and reopens automatically if the user later deletes a set to drop below the prescription.
    LaunchedEffect(isComplete) {
        expandedAfterComplete = false
    }
    val showInputRow = !isComplete && !stoppedEarly || expandedAfterComplete

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: exercise name + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EXERCISE ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (prescribedReps.isNotEmpty()) {
                        Text(
                            text = "PRESCRIBED: ${prescribedReps.joinToString(" / ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Completion checkmark — only for prescribed exercises where all sets are logged
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Stop-early X — shown only when the exercise has a prescription, isn't
                // complete, and hasn't already been stopped. Collapses the input row
                // without showing a checkmark. Reversible by tapping the + button.
                if (canStopEarly && !stoppedEarly) {
                    IconButton(onClick = { stoppedEarly = true }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Stop early",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sets table
            if (exercise.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SetsTable(
                    sets = exercise.sets,
                    onEditSet = onEditSet,
                    onDeleteSet = onDeleteSet
                )
            }

            // Input row for adding new sets — hidden once all prescribed sets are logged.
            // User can tap the small + button below to expand it for bonus sets.
            if (showInputRow) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = "REPS",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    StyledTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = "WEIGHT",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    FilledTonalButton(
                        onClick = {
                            if (reps.isNotBlank() && weight.isNotBlank()) {
                                onAddSet(reps, weight)
                                reps = ""
                                weight = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("ADD", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                // Compact + button in the bottom-right when the card is collapsed.
                // Reopens the input row whether the card was completed OR stopped early.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            expandedAfterComplete = true
                            stoppedEarly = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add another set",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetsTable(
    sets: List<SetEntry>,
    onEditSet: (setIndex: Int, reps: String, weight: String) -> Unit,
    onDeleteSet: (setIndex: Int) -> Unit
) {
    // -1 means no row is in edit mode
    var editingIndex by remember { mutableStateOf(-1) }

    Column {
        // Header row
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                text = "SET",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = "REPS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "WEIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            // Spacer for action icon column
            Spacer(modifier = Modifier.width(40.dp))
        }
        // Top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        // Set rows
        sets.forEachIndexed { i, set ->
            if (editingIndex == i) {
                EditableSetRow(
                    setNumber = i + 1,
                    initialReps = set.reps,
                    initialWeight = set.weight,
                    onSave = { reps, weight ->
                        onEditSet(i, reps, weight)
                        editingIndex = -1
                    },
                    onCancel = { editingIndex = -1 },
                    onDelete = {
                        onDeleteSet(i)
                        editingIndex = -1
                    }
                )
            } else {
                ReadOnlySetRow(
                    setNumber = i + 1,
                    set = set,
                    onTap = { editingIndex = i }
                )
            }
            if (i < sets.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

@Composable
private fun ReadOnlySetRow(
    setNumber: Int,
    set: SetEntry,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$setNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = set.reps,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${set.weight} lbs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "Edit set",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(40.dp)
                .size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableSetRow(
    setNumber: Int,
    initialReps: String,
    initialWeight: String,
    onSave: (reps: String, weight: String) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var reps by remember { mutableStateOf(initialReps) }
    var weight by remember { mutableStateOf(initialWeight) }
    var rowHasFocus by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    fun commit() {
        if (reps.isNotBlank() && weight.isNotBlank() &&
            (reps != initialReps || weight != initialWeight)
        ) {
            onSave(reps, weight)
        } else {
            onCancel()
        }
    }

    // Container-level focus tracking: only commit when focus leaves the entire row
    // (so tapping between reps and weight doesn't trigger a save mid-edit).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onFocusChanged { state ->
                val nowFocused = state.hasFocus
                if (rowHasFocus && !nowFocused) {
                    // Focus just left the entire row
                    commit()
                }
                rowHasFocus = nowFocused
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$setNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(34.dp)
        )
        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }
            ),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete set",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    maxLines: Int = 1,
    singleLine: Boolean = minLines == 1 && maxLines == 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

