package ca.sb.gymlogger.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ca.sb.gymlogger.data.SessionImporter
import ca.sb.gymlogger.ui.components.AppCard
import ca.sb.gymlogger.ui.components.SectionLabel
import ca.sb.gymlogger.ui.components.StatTile
import ca.sb.gymlogger.ui.viewmodel.ExportViewModel
import ca.sb.gymlogger.ui.viewmodel.ImportViewModel
import ca.sb.gymlogger.ui.viewmodel.SummaryViewModel
import ca.sb.gymlogger.ui.viewmodel.workoutViewModel

@Composable
fun SummaryScreen(onOpenDrawer: () -> Unit = {}) {
    val viewModel: SummaryViewModel = workoutViewModel { SummaryViewModel(it) }
    val exportVM: ExportViewModel = workoutViewModel { ExportViewModel(it) }
    val importVM: ImportViewModel = workoutViewModel { ImportViewModel(it) }
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current

    // Holds the Uri the user picked, while we ask Replace-or-Merge.
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp)
    ) {
        // Header row: hamburger menu on the left, "SUMMARY" eyebrow on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "SUMMARY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "All-time stats",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Top stat row: days + workouts + sets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                label = "Days",
                value = stats.totalWorkoutDays.toString(),
                valueIsAccent = true,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Workouts",
                value = stats.totalWorkouts.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Sets",
                value = stats.totalSets.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Volume — full-width emphasis tile
        StatTile(
            label = "Total Volume (lbs)",
            value = formatNumber(stats.totalVolume),
            valueIsAccent = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Breakdown by type
        SectionLabel("By Type")
        Spacer(modifier = Modifier.height(10.dp))

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                TypeRow("Weights", stats.liftingCount)
                DividerLine()
                TypeRow("Cardio", stats.cardioCount)
                DividerLine()
                TypeRow("Stretching", stats.stretchingCount)
            }
        }

        if (stats.totalWorkouts == 0) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log a workout to start seeing stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Backup / Export ──
        Spacer(modifier = Modifier.height(36.dp))
        SectionLabel("Backup")
        Spacer(modifier = Modifier.height(10.dp))

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Export your data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Builds a JSON file (for backup or import elsewhere) and a Markdown summary, " +
                        "then opens the share sheet — send to Gmail, Drive, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        exportVM.export(context) { result ->
                            // Build an Intent.ACTION_SEND_MULTIPLE with both files attached
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "*/*"
                                putParcelableArrayListExtra(
                                    Intent.EXTRA_STREAM,
                                    arrayListOf(result.jsonUri, result.markdownUri)
                                )
                                putExtra(Intent.EXTRA_SUBJECT, result.displayName)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "GymLogger workout export — JSON and Markdown attached."
                                )
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(intent, "Share workout data").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                        }
                    },
                    enabled = !exportVM.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (exportVM.isExporting) "PREPARING…" else "EXPORT & SHARE",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // ── Import ──
        Spacer(modifier = Modifier.height(16.dp))

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Import data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Load a GymLogger .json export file. Useful for restoring " +
                        "from a backup or moving to a new device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        // Launch the system file picker. We accept any file ("*/*") because
                        // file managers register .json files inconsistently — some as
                        // application/json, some as text/plain, some as */*. The importer
                        // validates the actual JSON content and fails cleanly on bad input.
                        filePickerLauncher.launch("*/*")
                    },
                    enabled = !importVM.isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Filled.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (importVM.isImporting) "IMPORTING…" else "CHOOSE FILE",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    exportVM.exportError?.let { error ->
        AlertDialog(
            onDismissRequest = { exportVM.clearError() },
            title = { Text("Export failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { exportVM.clearError() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // After file pick: ask Replace or Merge.
    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import sessions") },
            text = {
                Text(
                    "How should this be combined with your current data?\n\n" +
                        "Replace: erase everything currently logged and load only what's in the file.\n\n" +
                        "Merge: keep existing sessions and add the imported ones on top."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importVM.import(context, uri, SessionImporter.Mode.REPLACE)
                    pendingImportUri = null
                }) {
                    Text("REPLACE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        importVM.import(context, uri, SessionImporter.Mode.MERGE)
                        pendingImportUri = null
                    }) {
                        Text("MERGE", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { pendingImportUri = null }) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    importVM.lastResult?.let { result ->
        AlertDialog(
            onDismissRequest = { importVM.clearResult() },
            title = { Text("Import complete") },
            text = { Text(result.summary()) },
            confirmButton = {
                TextButton(onClick = { importVM.clearResult() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    importVM.importError?.let { error ->
        AlertDialog(
            onDismissRequest = { importVM.clearError() },
            title = { Text("Import failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { importVM.clearError() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun TypeRow(name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/** Insert thousand separators. 12345 → "12,345" */
private fun formatNumber(n: Int): String =
    if (n == 0) "—" else "%,d".format(n)
