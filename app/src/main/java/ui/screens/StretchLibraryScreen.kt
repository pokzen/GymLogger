package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.data.LibraryStretch
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.PrimaryActionButton
import ca.bpmproperty.gymlogger.ui.viewmodel.StretchLibraryViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchLibraryScreen(onBack: () -> Unit) {
    val viewModel: StretchLibraryViewModel = workoutViewModel { StretchLibraryViewModel(it) }
    val stretches by viewModel.stretches.collectAsState()

    var editorTarget by remember { mutableStateOf<StretchEditorTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<LibraryStretch?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "STRETCH LIBRARY",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editorTarget = StretchEditorTarget.New },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add stretch")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (stretches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your stretch library is empty.\nAdd one with the + button, or add via a session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(stretches, key = { it.id }) { stretch ->
                StretchRow(
                    stretch = stretch,
                    onEdit = { editorTarget = StretchEditorTarget.Edit(stretch) },
                    onDelete = { deleteTarget = stretch }
                )
            }
        }
    }

    editorTarget?.let { target ->
        StretchEditorDialog(
            target = target,
            onSave = { name ->
                when (target) {
                    StretchEditorTarget.New -> viewModel.addStretch(name)
                    is StretchEditorTarget.Edit -> viewModel.updateStretch(target.stretch, name)
                }
                editorTarget = null
            },
            onCancel = { editorTarget = null }
        )
    }

    deleteTarget?.let { stretch ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete stretch?") },
            text = { Text("\"${stretch.name}\" will be removed. Past sessions are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStretch(stretch)
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

private sealed class StretchEditorTarget {
    object New : StretchEditorTarget()
    data class Edit(val stretch: LibraryStretch) : StretchEditorTarget()
}

@Composable
private fun StretchRow(
    stretch: LibraryStretch,
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
                text = stretch.name,
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
private fun StretchEditorDialog(
    target: StretchEditorTarget,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    val initialName = when (target) {
        StretchEditorTarget.New -> ""
        is StretchEditorTarget.Edit -> target.stretch.name
    }
    val title = when (target) {
        StretchEditorTarget.New -> "NEW STRETCH"
        is StretchEditorTarget.Edit -> "EDIT STRETCH"
    }
    var name by remember { mutableStateOf(initialName) }

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
            StyledTextField(
                value = name,
                onValueChange = { name = it },
                label = "NAME",
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            PrimaryActionButton(
                text = "Save",
                onClick = { onSave(name) },
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
