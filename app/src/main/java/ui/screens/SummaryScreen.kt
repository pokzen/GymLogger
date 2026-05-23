package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.SectionLabel
import ca.bpmproperty.gymlogger.ui.components.StatTile
import ca.bpmproperty.gymlogger.ui.viewmodel.SummaryViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@Composable
fun SummaryScreen() {
    val viewModel: SummaryViewModel = workoutViewModel { SummaryViewModel(it) }
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "SUMMARY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
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
