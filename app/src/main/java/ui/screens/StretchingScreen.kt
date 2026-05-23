package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items

val defaultStretches = listOf(
    "Hip Flexor Stretch", "Hamstring Stretch", "Quad Stretch",
    "Chest Opener", "Shoulder Cross-Body", "Tricep Stretch",
    "Pigeon Pose", "Child's Pose", "Cat-Cow",
    "Spinal Twist", "Calf Stretch", "Neck Rolls"
)

data class StretchEntry(val name: String, val duration: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchingScreen(onBack: () -> Unit = {}) {
    val stretches = remember { mutableStateListOf<StretchEntry>() }
    var showStretchPicker by remember { mutableStateOf(false) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stretching Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showSaveConfirmation = true },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = stretches.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showStretchPicker = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Stretch")
            }
        }
    ) { innerPadding ->
        if (stretches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap + to add a stretch",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stretches.size) { index ->
                    StretchCard(
                        stretch = stretches[index],
                        onDelete = { stretches.removeAt(index) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Session notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }
        }
    }

    if (showStretchPicker) {
        StretchPickerDialog(
            onDismiss = { showStretchPicker = false },
            onSelect = { name, duration ->
                stretches.add(StretchEntry(name, duration))
                showStretchPicker = false
            }
        )
    }

    if (showSaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmation = false },
            title = { Text("Session Saved") },
            text = { Text("Stretching session logged! (Database coming soon)") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveConfirmation = false
                    onBack()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun StretchCard(
    stretch: StretchEntry,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stretch.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (stretch.duration.isNotBlank()) {
                    Text(
                        text = "${stretch.duration} seconds",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StretchPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    var customName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stretch") },
        text = {
            Column {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Custom stretch name") },
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
                    items(defaultStretches) { stretch ->
                        TextButton(
                            onClick = { onSelect(stretch, duration) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stretch)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (customName.isNotBlank()) onSelect(customName, duration)
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