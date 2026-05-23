package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.components.LogOnDateSheet
import ca.bpmproperty.gymlogger.ui.components.WorkoutType
import ca.bpmproperty.gymlogger.ui.components.WorkoutWeekStrip
import ca.bpmproperty.gymlogger.ui.viewmodel.CalendarViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.IconButton

@Composable
fun LogScreen(
    navController: NavController,
    onOpenDrawer: () -> Unit = {}
) {
    val calendarVM: CalendarViewModel = workoutViewModel { CalendarViewModel(it) }
    val calData by calendarVM.data.collectAsState()

    var sheetForDate by remember { mutableStateOf<Int?>(null) }
    var quickMarkConfirmDate by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        // Header row: hamburger menu on the left, "SHANE'S LOG" on the right.
        // Bottom-align so the text's baseline sits flush with the bottom of the
        // hamburger glyph.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.Bottom
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
                text = "SHANE'S LOG",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp, bottom = 12.dp)
            )
        }
        Text(
            text = "What's today?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 36.dp)
        )

        // ── Workout type cards ──
        WorkoutTypeCard(
            title = "Cardio",
            subtitle = "Treadmill, distance & pace",
            icon = Icons.Filled.DirectionsRun,
            onClick = { navController.navigate("cardio") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        WorkoutTypeCard(
            title = "Weights",
            subtitle = "Sets, reps & weight",
            icon = Icons.Filled.FitnessCenter,
            onClick = { navController.navigate("weights_picker") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        WorkoutTypeCard(
            title = "Stretching",
            subtitle = "Mobility & recovery",
            icon = Icons.Filled.SelfImprovement,
            onClick = { navController.navigate("stretching") }
        )

        Spacer(modifier = Modifier.height(64.dp))

        // ── Current-week strip — tap to open the full scrollable calendar ──
        WorkoutWeekStrip(
            workoutDates = calData.workoutDates,
            onOpenCalendar = { navController.navigate("calendar_popout") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // ── Streak strip ──
        StreakStrip(
            currentStreak = calData.currentStreak,
            daysInLast30 = calData.daysInLast30,
            modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
        )
    }

    // Bottom sheet for picking type when an empty day is tapped
    sheetForDate?.let { dateKey ->
        LogOnDateSheet(
            dateKey = dateKey,
            onTypeSelected = { type ->
                val route = when (type) {
                    WorkoutType.CARDIO -> "cardio?date=$dateKey"
                    WorkoutType.WEIGHTS -> "lifting?date=$dateKey"
                    WorkoutType.STRETCHING -> "stretching?date=$dateKey"
                }
                sheetForDate = null
                navController.navigate(route)
            },
            onDismiss = { sheetForDate = null }
        )
    }

    // Quick-mark confirmation (triggered by long-pressing an empty past/today day)
    quickMarkConfirmDate?.let { dateKey ->
        AlertDialog(
            onDismissRequest = { quickMarkConfirmDate = null },
            title = { Text("Mark as worked out?") },
            text = {
                Text(
                    "This will flag ${ca.bpmproperty.gymlogger.data.dayLabel(dateKey)} on the calendar without logging any details. " +
                        "You can remove the mark later from History."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    calendarVM.markDayQuick(dateKey)
                    quickMarkConfirmDate = null
                }) {
                    Text("MARK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { quickMarkConfirmDate = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun StreakStrip(
    currentStreak: Int,
    daysInLast30: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Past-month line on top.
        Text(
            text = "Past Month: $daysInLast30 / 30 days (${"%.2f".format(daysInLast30 * 100.0 / 30.0)}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Streak line below, with flame icon.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (currentStreak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (currentStreak > 0) "$currentStreak day streak" else "No streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WorkoutTypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
