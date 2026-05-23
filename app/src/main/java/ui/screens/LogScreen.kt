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
import ca.bpmproperty.gymlogger.ui.components.WorkoutCalendar
import ca.bpmproperty.gymlogger.ui.components.WorkoutType
import ca.bpmproperty.gymlogger.ui.viewmodel.CalendarViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@Composable
fun LogScreen(navController: NavController) {
    val calendarVM: CalendarViewModel = workoutViewModel { CalendarViewModel(it) }
    val calData by calendarVM.data.collectAsState()

    var sheetForDate by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp)
    ) {
        Text(
            text = "SHANE'S LOG",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 12.dp)
        )
        Text(
            text = "What's today?",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 20.dp)
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

        // ── Calendar heatmap ──
        WorkoutCalendar(
            workoutDates = calData.workoutDates,
            onDayTap = { dateKey, hasWorkout ->
                if (hasWorkout) {
                    // Switch to the History bottom-tab and ask it to scroll to this day.
                    navController.navigate("history") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    navController.getBackStackEntry("history")
                        .savedStateHandle["scrollToDateKey"] = dateKey
                } else {
                    // Open the "log on this date" sheet
                    sheetForDate = dateKey
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // ── Streak strip ──
        StreakStrip(
            currentStreak = calData.currentStreak,
            daysThisMonth = calData.daysThisMonth,
            modifier = Modifier.padding(bottom = 8.dp)
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
}

@Composable
private fun StreakStrip(
    currentStreak: Int,
    daysThisMonth: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$daysThisMonth this month",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
