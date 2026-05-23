package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SetEntry(val reps: String, val weight: String)

data class ExerciseEntry(val name: String, val sets: List<SetEntry>)

val defaultExercises = listOf(
    "Bench Press", "Squat", "Deadlift", "Overhead Press",
    "Barbell Row", "Pull-Up", "Dumbbell Curl", "Tricep Pushdown",
    "Leg Press", "Lat Pulldown", "Incline Press", "Cable Row"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftingScreen(onBack: () -> Unit = {}) {
    val exercises = remember { mutableStateListOf<ExerciseEntry>() }
    var showExercisePicker by remember { mutableStateOf(false) }
    var showSaveConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lifting Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showSaveConfirmation = true },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = exercises.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showExercisePicker = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
            }
        }
    ) { innerPadding ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap + to add an exercise",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(exercises.size) { index ->
                    ExerciseCard(
                        exercise = exercises[index],
                        onAddSet = { reps, weight ->
                            val updated = exercises[index].copy(
                                sets = exercises[index].sets + SetEntry(reps, weight)
                            )
                            exercises[index] = updated
                        },
                        onDelete = { exercises.removeAt(index) }
                    )
                }
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            onDismiss = { showExercisePicker = false },
            onSelect = { name ->
                exercises.add(ExerciseEntry(name, emptyList()))
                showExercisePicker = false
            }
        )
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text("Save Session") },
            text = { Text("Session saved! (Database coming soon)") },
            confirmButton = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseEntry,
    onAddSet: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (exercise.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                exercise.sets.forEachIndexed { i, set ->
                    Text(
                        text = "Set ${i + 1}: ${set.reps} reps @ ${set.weight} lbs",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (lbs)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (reps.isNotBlank() && weight.isNotBlank()) {
                            onAddSet(reps, weight)
                            reps = ""
                            weight = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
fun ExercisePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Custom exercise name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Or pick one:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(defaultExercises) { exercise ->
                        TextButton(
                            onClick = { onSelect(exercise) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(exercise)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (customName.isNotBlank()) onSelect(customName)
                },
                enabled = customName.isNotBlank()
            ) {
                Text("Add Custom")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}