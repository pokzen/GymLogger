package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.data.TemplateExercise
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.ExercisePicker
import ca.bpmproperty.gymlogger.ui.components.PrimaryActionButton
import ca.bpmproperty.gymlogger.ui.viewmodel.MUSCLE_GROUP_ORDER
import ca.bpmproperty.gymlogger.ui.viewmodel.TemplateViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    templateId: Int?,           // null or 0 = new template
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: TemplateViewModel = workoutViewModel { TemplateViewModel(it) }

    var name by remember { mutableStateOf("") }
    val exercises = remember { mutableStateListOf<TemplateExercise>() }
    var loaded by remember { mutableStateOf(false) }
    var editingExerciseIndex by remember { mutableStateOf(-1) } // -1 closed, -2 adding, >=0 editing
    var showExercisePicker by remember { mutableStateOf(false) }
    // Holds the (name, muscleGroup) pair when picked, to seed the editor card
    var pickerPrefill by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Load existing template once
    LaunchedEffect(templateId) {
        if (templateId != null && templateId > 0 && !loaded) {
            viewModel.loadTemplate(templateId) { tv ->
                if (tv != null) {
                    name = tv.name
                    exercises.clear()
                    exercises.addAll(tv.exercises)
                }
                loaded = true
            }
        } else {
            loaded = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (templateId != null && templateId > 0) "EDIT TEMPLATE" else "NEW TEMPLATE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    PrimaryActionButton(
                        text = "Save",
                        onClick = {
                            viewModel.saveTemplate(
                                id = templateId ?: 0,
                                name = name.trim().ifBlank { "Untitled" },
                                exercises = exercises.toList()
                            ) {
                                onSaved()
                            }
                        },
                        enabled = name.isNotBlank() && exercises.isNotEmpty(),
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Template name
            StyledTextField(
                value = name,
                onValueChange = { name = it },
                label = "TEMPLATE NAME",
                modifier = Modifier.fillMaxWidth()
            )

            // Exercises section
            Text(
                text = "EXERCISES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (exercises.isEmpty() && editingExerciseIndex == -1) {
                Text(
                    text = "Add at least one exercise to save this template.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Render exercises with muscle-group headers inserted between groups.
            // We walk the list in order (already kept group-ordered by insertion logic);
            // whenever the muscle group changes vs. the previous item, we render a header.
            // The user's "real index" within the underlying `exercises` list is what's
            // used for edit, delete, and up/down operations.
            var lastGroup: String? = null
            exercises.forEachIndexed { index, ex ->
                val groupLabel = ex.muscleGroup.ifBlank { "Other" }
                if (groupLabel != lastGroup) {
                    lastGroup = groupLabel
                    Text(
                        text = groupLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (editingExerciseIndex == index) {
                    ExerciseEditor(
                        initial = ex,
                        onSave = { updated ->
                            exercises[index] = updated
                            // If the muscle group changed, re-sort so the group invariant holds
                            if (updated.muscleGroup != ex.muscleGroup) {
                                resortByGroup(exercises)
                            }
                            editingExerciseIndex = -1
                        },
                        onCancel = { editingExerciseIndex = -1 },
                        onDelete = {
                            exercises.removeAt(index)
                            editingExerciseIndex = -1
                        },
                        isNew = false,
                        showMoveUp = canMoveUpWithinGroup(exercises, index),
                        showMoveDown = canMoveDownWithinGroup(exercises, index),
                        onMoveUp = {
                            if (canMoveUpWithinGroup(exercises, index)) {
                                val item = exercises.removeAt(index)
                                exercises.add(index - 1, item)
                                editingExerciseIndex = index - 1
                            }
                        },
                        onMoveDown = {
                            if (canMoveDownWithinGroup(exercises, index)) {
                                val item = exercises.removeAt(index)
                                exercises.add(index + 1, item)
                                editingExerciseIndex = index + 1
                            }
                        }
                    )
                } else {
                    ExerciseRow(
                        index = index,
                        exercise = ex,
                        onTap = { editingExerciseIndex = index }
                    )
                }
            }

            // New exercise editor (opens after picker has chosen a name/muscle group)
            if (editingExerciseIndex == -2) {
                val seed = pickerPrefill
                ExerciseEditor(
                    initial = TemplateExercise(
                        name = seed?.first ?: "",
                        muscleGroup = seed?.second ?: "",
                        prescribedReps = emptyList()
                    ),
                    onSave = { newEx ->
                        // Insert at the end of the new exercise's muscle group so the
                        // group-ordered invariant is maintained.
                        insertRespectingGroups(exercises, newEx)
                        editingExerciseIndex = -1
                        pickerPrefill = null
                    },
                    onCancel = {
                        editingExerciseIndex = -1
                        pickerPrefill = null
                    },
                    onDelete = null,
                    isNew = true,
                    showMoveUp = false,
                    showMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }

            // Add exercise button — opens the library picker
            if (editingExerciseIndex == -1) {
                FilledTonalButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD EXERCISE", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // Library-backed exercise picker
    if (showExercisePicker) {
        ExercisePicker(
            onSelect = { pickedName, pickedGroup ->
                pickerPrefill = pickedName to pickedGroup
                showExercisePicker = false
                editingExerciseIndex = -2
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

@Composable
private fun ExerciseRow(
    index: Int,
    exercise: TemplateExercise,
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
                    text = "${index + 1}. ${exercise.name.ifBlank { "Untitled" }}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val tags = buildList {
                    if (exercise.muscleGroup.isNotBlank()) add(exercise.muscleGroup)
                    if (exercise.prescribedReps.isNotEmpty()) {
                        add(exercise.prescribedReps.joinToString("/") + " reps")
                    }
                }
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseEditor(
    initial: TemplateExercise,
    onSave: (TemplateExercise) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?,
    isNew: Boolean,
    showMoveUp: Boolean,
    showMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var muscleGroup by remember { mutableStateOf(initial.muscleGroup) }
    var repsText by remember {
        mutableStateOf(initial.prescribedReps.joinToString(", "))
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isNew) "NEW EXERCISE" else "EDIT EXERCISE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            StyledTextField(
                value = name,
                onValueChange = { name = it },
                label = "EXERCISE NAME",
                modifier = Modifier.fillMaxWidth()
            )

            StyledTextField(
                value = muscleGroup,
                onValueChange = { muscleGroup = it },
                label = "MUSCLE GROUP (E.G. CHEST, TRICEPS)",
                modifier = Modifier.fillMaxWidth()
            )

            StyledTextField(
                value = repsText,
                onValueChange = { repsText = it },
                label = "REPS PER SET (E.G. 12, 10, 8)",
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Comma-separated. Number of values = number of sets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete exercise",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (showMoveUp) {
                    IconButton(onClick = onMoveUp) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showMoveDown) {
                    IconButton(onClick = onMoveDown) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Move down",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSave(
                            TemplateExercise(
                                name = name.trim(),
                                muscleGroup = muscleGroup.trim(),
                                prescribedReps = parseRepsList(repsText)
                            )
                        )
                    }
                )
            }
        }
    }
}

/** Parse "12, 10, 8" → [12, 10, 8]. Tolerates whitespace and skips non-numeric tokens. */
private fun parseRepsList(text: String): List<Int> =
    text.split(',', ';', ' ', '/')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }

// ---- Muscle-group ordering helpers ----

/**
 * Insert [newEx] at the end of its muscle group, maintaining the group-ordered invariant.
 * If the group doesn't exist yet, insert at the position where it would go per
 * [MUSCLE_GROUP_ORDER].
 */
private fun insertRespectingGroups(
    exercises: androidx.compose.runtime.snapshots.SnapshotStateList<TemplateExercise>,
    newEx: TemplateExercise
) {
    val newExGroupRank = groupRank(newEx.muscleGroup)

    // Find the last index of an exercise in the same group; insert right after it
    val lastSameGroup = exercises.indexOfLast { it.muscleGroup == newEx.muscleGroup }
    if (lastSameGroup >= 0) {
        exercises.add(lastSameGroup + 1, newEx)
        return
    }

    // No existing exercise in this group — insert at the position dictated by group order
    val insertIdx = exercises.indexOfFirst { groupRank(it.muscleGroup) > newExGroupRank }
    if (insertIdx < 0) {
        exercises.add(newEx)
    } else {
        exercises.add(insertIdx, newEx)
    }
}

/** Re-sort the list in place by group order, preserving within-group order. */
private fun resortByGroup(
    exercises: androidx.compose.runtime.snapshots.SnapshotStateList<TemplateExercise>
) {
    // Stable sort by group rank — items within the same group keep their relative order
    val sorted = exercises.toList().sortedBy { groupRank(it.muscleGroup) }
    exercises.clear()
    exercises.addAll(sorted)
}

/** Index of [group] in [MUSCLE_GROUP_ORDER], or one past the end if unknown. */
private fun groupRank(group: String): Int {
    val idx = MUSCLE_GROUP_ORDER.indexOf(group.ifBlank { "Other" })
    return if (idx >= 0) idx else MUSCLE_GROUP_ORDER.size
}

/** True if the item at [index] can be moved up while staying inside its muscle group. */
private fun canMoveUpWithinGroup(exercises: List<TemplateExercise>, index: Int): Boolean {
    if (index <= 0) return false
    return exercises[index].muscleGroup == exercises[index - 1].muscleGroup
}

/** True if the item at [index] can be moved down while staying inside its muscle group. */
private fun canMoveDownWithinGroup(exercises: List<TemplateExercise>, index: Int): Boolean {
    if (index >= exercises.size - 1) return false
    return exercises[index].muscleGroup == exercises[index + 1].muscleGroup
}
