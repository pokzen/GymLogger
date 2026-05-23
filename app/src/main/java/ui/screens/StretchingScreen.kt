package ca.bpmproperty.gymlogger.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.data.StretchEntry
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.AppDatePickerDialog
import ca.bpmproperty.gymlogger.ui.components.DateChip
import ca.bpmproperty.gymlogger.ui.components.DraftRestoredBanner
import ca.bpmproperty.gymlogger.ui.components.PrimaryActionButton
import ca.bpmproperty.gymlogger.ui.viewmodel.StretchLibraryViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.StretchingViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchingScreen(
    onBack: () -> Unit = {},
    initialDateKey: Int? = null,
    editingSessionId: Int? = null,
    onManageLibrary: () -> Unit = {}
) {
    val viewModel: StretchingViewModel = workoutViewModel { StretchingViewModel(it) }
    val stretches = viewModel.stretches
    var showStretchPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

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
                        text = "STRETCHING",
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
                    IconButton(onClick = onManageLibrary) {
                        Icon(
                            Icons.Filled.LibraryBooks,
                            contentDescription = "Stretch library",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    PrimaryActionButton(
                        text = if (viewModel.isSaving) "Saving" else "Finish",
                        onClick = { viewModel.save(onDone = onBack) },
                        enabled = stretches.isNotEmpty() && !viewModel.isSaving,
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
                onClick = { showStretchPicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Stretch")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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

            if (stretches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EMPTY SESSION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add a stretch",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stretches.size) { index ->
                        StretchCard(
                            stretch = stretches[index],
                            onEdit = { name, duration ->
                                viewModel.editStretch(index, name, duration)
                            },
                            onDelete = { viewModel.removeStretch(index) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledTextField(
                            value = viewModel.notes,
                            onValueChange = { viewModel.notes = it },
                            label = "SESSION NOTES",
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
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
            title = { Text("Discard in-progress session?") },
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

    if (showStretchPicker) {
        StretchPickerDialog(
            onDismiss = { showStretchPicker = false },
            onSelect = { name, duration ->
                viewModel.addStretch(name, duration)
                showStretchPicker = false
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchCard(
    stretch: StretchEntry,
    onEdit: (name: String, duration: String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(stretch) { mutableStateOf(stretch.name) }
    var editDuration by remember(stretch) { mutableStateOf(stretch.duration) }
    var rowHasFocus by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    fun commit() {
        val changed = editName != stretch.name || editDuration != stretch.duration
        if (editName.isNotBlank() && changed) {
            onEdit(editName, editDuration)
        }
        isEditing = false
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        if (isEditing) {
            // EDIT MODE — commit on container-level focus loss
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        val nowFocused = state.hasFocus
                        if (rowHasFocus && !nowFocused) commit()
                        rowHasFocus = nowFocused
                    }
            ) {
                StyledTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = "NAME",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                "DURATION (SECONDS)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        }),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            // READ MODE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stretch.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (stretch.duration.isNotBlank()) {
                        Text(
                            text = "${stretch.duration} SECONDS",
                            style = MaterialTheme.typography.labelSmall,
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
}

@Composable
fun StretchPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    val libraryVM: StretchLibraryViewModel = workoutViewModel { StretchLibraryViewModel(it) }
    val library by libraryVM.stretches.collectAsState()

    var customName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "ADD STRETCH",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                StyledTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = "DURATION (SECONDS)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                StyledTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = "NAME",
                    modifier = Modifier.fillMaxWidth()
                )
                if (library.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "OR PICK ONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(library, key = { it.id }) { stretch ->
                            TextButton(
                                onClick = { onSelect(stretch.name, duration) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = stretch.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (customName.isNotBlank()) {
                        // Auto-save to library (idempotent — case-insensitive duplicate is a no-op)
                        libraryVM.addStretch(customName)
                        onSelect(customName, duration)
                    }
                },
                enabled = customName.isNotBlank()
            ) {
                Text(
                    "ADD",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCEL",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
