package ca.sb.gymlogger.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.sb.gymlogger.data.LibraryExercise
import ca.sb.gymlogger.ui.viewmodel.ExerciseGroup
import ca.sb.gymlogger.ui.viewmodel.LibraryViewModel
import ca.sb.gymlogger.ui.viewmodel.MUSCLE_GROUP_ORDER
import ca.sb.gymlogger.ui.viewmodel.workoutViewModel

/**
 * Full-screen exercise picker. Groups exercises by muscle group, supports search,
 * lets the user add a custom (one-off) exercise that's also saved to the library.
 *
 * @param onSelect called with (name, muscleGroup) when an exercise is picked
 * @param onDismiss called when the picker is closed without selecting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePicker(
    onSelect: (name: String, muscleGroup: String) -> Unit,
    onDismiss: () -> Unit
) {
    val viewModel: LibraryViewModel = workoutViewModel { LibraryViewModel(it) }
    val groups by viewModel.grouped.collectAsState()

    var query by remember { mutableStateOf("") }
    var customMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customGroup by remember { mutableStateOf("Chest") }
    var groupMenuOpen by remember { mutableStateOf(false) }

    // Filter groups by query
    val filtered: List<ExerciseGroup> = remember(groups, query) {
        if (query.isBlank()) groups
        else groups.mapNotNull { g ->
            val matches = g.exercises.filter { it.name.contains(query, ignoreCase = true) }
            if (matches.isEmpty()) null else g.copy(exercises = matches)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (customMode) "ADD CUSTOM EXERCISE" else "CHOOSE EXERCISE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (customMode) {
                    // ── Custom-add mode ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("EXERCISE NAME", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = pickerFieldColors()
                        )
                        ExposedDropdownMenuBox(
                            expanded = groupMenuOpen,
                            onExpandedChange = { groupMenuOpen = !groupMenuOpen }
                        ) {
                            OutlinedTextField(
                                value = customGroup,
                                onValueChange = { customGroup = it },
                                label = { Text("MUSCLE GROUP", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = pickerFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = groupMenuOpen,
                                onDismissRequest = { groupMenuOpen = false },
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                MUSCLE_GROUP_ORDER.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(option, color = MaterialTheme.colorScheme.onSurface)
                                        },
                                        onClick = {
                                            customGroup = option
                                            groupMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { customMode = false }) {
                                Text(
                                    "BACK",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            PrimaryActionButton(
                                text = "Add & Use",
                                onClick = {
                                    val trimmedName = customName.trim()
                                    if (trimmedName.isNotBlank()) {
                                        // Save to library AND select for this session
                                        viewModel.addExercise(trimmedName, customGroup)
                                        onSelect(trimmedName, customGroup)
                                    }
                                },
                                enabled = customName.isNotBlank()
                            )
                        }
                    }
                } else {
                    // ── Browse mode ──
                    // Search field
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search exercises…") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(12.dp),
                        colors = pickerFieldColors()
                    )

                    // Exercise list, grouped
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (query.isBlank())
                                            "Library is empty."
                                        else
                                            "No matches for \"$query\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        filtered.forEach { group ->
                            item(key = "h-${group.muscleGroup}") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = group.muscleGroup.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            items(group.exercises, key = { "e-${it.id}" }) { exercise ->
                                PickerRow(
                                    exercise = exercise,
                                    onTap = { onSelect(exercise.name, exercise.muscleGroup) }
                                )
                            }
                        }
                    }

                    // Footer — switch to custom mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { customMode = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("+ ADD CUSTOM EXERCISE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(exercise: LibraryExercise, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun pickerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)
