package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.data.LibraryExercise
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.PrimaryActionButton
import ca.bpmproperty.gymlogger.ui.viewmodel.LibraryViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.MUSCLE_GROUP_ORDER
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(onBack: () -> Unit) {
    val viewModel: LibraryViewModel = workoutViewModel { LibraryViewModel(it) }
    val groups by viewModel.grouped.collectAsState()

    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<LibraryExercise?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EXERCISE LIBRARY",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editorTarget = EditorTarget.New },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add exercise")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your library is empty. Tap + to add an exercise.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            groups.forEach { group ->
                item(key = "header-${group.muscleGroup}") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = group.muscleGroup.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(group.exercises, key = { "ex-${it.id}" }) { exercise ->
                    LibraryRow(
                        exercise = exercise,
                        onEdit = { editorTarget = EditorTarget.Edit(exercise) },
                        onDelete = { deleteTarget = exercise }
                    )
                }
            }
        }
    }

    editorTarget?.let { target ->
        LibraryEditorDialog(
            target = target,
            onSave = { name, group ->
                when (target) {
                    EditorTarget.New -> viewModel.addExercise(name, group)
                    is EditorTarget.Edit -> viewModel.updateExercise(
                        target.exercise.copy(name = name.trim(), muscleGroup = group.trim().ifBlank { "Other" })
                    )
                }
                editorTarget = null
            },
            onCancel = { editorTarget = null }
        )
    }

    deleteTarget?.let { exercise ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete exercise?") },
            text = { Text("\"${exercise.name}\" will be removed from your library. Past sessions and templates are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExercise(exercise)
                    deleteTarget = null
                }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

private sealed class EditorTarget {
    object New : EditorTarget()
    data class Edit(val exercise: LibraryExercise) : EditorTarget()
}

@Composable
private fun LibraryRow(
    exercise: LibraryExercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryEditorDialog(
    target: EditorTarget,
    onSave: (name: String, muscleGroup: String) -> Unit,
    onCancel: () -> Unit
) {
    val initialName: String
    val initialGroup: String
    val title: String
    when (target) {
        EditorTarget.New -> {
            initialName = ""
            initialGroup = "Chest"
            title = "NEW EXERCISE"
        }
        is EditorTarget.Edit -> {
            initialName = target.exercise.name
            initialGroup = target.exercise.muscleGroup
            title = "EDIT EXERCISE"
        }
    }
    var name by remember { mutableStateOf(initialName) }
    var group by remember { mutableStateOf(initialGroup) }
    var groupMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StyledTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "EXERCISE NAME",
                    modifier = Modifier.fillMaxWidth()
                )

                // Muscle group dropdown
                ExposedDropdownMenuBox(
                    expanded = groupMenuOpen,
                    onExpandedChange = { groupMenuOpen = !groupMenuOpen }
                ) {
                    StyledTextField(
                        value = group,
                        onValueChange = { group = it },
                        label = "MUSCLE GROUP",
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = groupMenuOpen,
                        onDismissRequest = { groupMenuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MUSCLE_GROUP_ORDER.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    group = option
                                    groupMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(
                text = "Save",
                onClick = { onSave(name, group) },
                enabled = name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    "CANCEL",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
