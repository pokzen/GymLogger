package ca.bpmproperty.gymlogger.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.SectionLabel
import ca.bpmproperty.gymlogger.ui.components.StatTile
import ca.bpmproperty.gymlogger.ui.viewmodel.ExportViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.SummaryViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@Composable
fun SummaryScreen(onOpenDrawer: () -> Unit = {}) {
    val viewModel: SummaryViewModel = workoutViewModel { SummaryViewModel(it) }
    val exportVM: ExportViewModel = workoutViewModel { ExportViewModel(it) }
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current

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
                    text = "Builds a JSON file (for Claude review) and a Markdown summary, " +
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
