package ca.bpmproperty.gymlogger.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ca.bpmproperty.gymlogger.data.dayLabel
import ca.bpmproperty.gymlogger.data.formatDateKeyShort
import ca.bpmproperty.gymlogger.data.isToday
import ca.bpmproperty.gymlogger.ui.components.AppCard
import ca.bpmproperty.gymlogger.ui.viewmodel.CardioEntry
import ca.bpmproperty.gymlogger.ui.viewmodel.HistoryViewModel
import ca.bpmproperty.gymlogger.ui.viewmodel.LiftingEntry
import ca.bpmproperty.gymlogger.ui.viewmodel.StretchingEntry
import ca.bpmproperty.gymlogger.ui.viewmodel.WorkoutDay
import ca.bpmproperty.gymlogger.ui.viewmodel.workoutViewModel

@Composable
fun HistoryScreen(
    scrollToDateKey: Int? = null,
    onOpenDrawer: () -> Unit = {},
    onEditLifting: (Int) -> Unit = {},
    onEditCardio: (Int) -> Unit = {},
    onEditStretching: (Int) -> Unit = {}
) {
    val viewModel: HistoryViewModel = workoutViewModel { HistoryViewModel(it) }
    val days by viewModel.days.collectAsState()

    // Pending-delete confirmation. Each is set by clicking trash on the relevant section.
    var deleteLiftingId by remember { mutableStateOf<Int?>(null) }
    var deleteCardioId by remember { mutableStateOf<Int?>(null) }
    var deleteStretchingId by remember { mutableStateOf<Int?>(null) }
    var deleteQuickLogDateKey by remember { mutableStateOf<Int?>(null) }

    // Has the user logged anything ever? (Either sessions or quick-marks.)
    val hasAnyData = days.any { !it.isFullyEmpty }

    val listState = rememberLazyListState()

    // The day currently highlighted (briefly) from a jump-to-day navigation.
    var highlightedDateKey by remember { mutableStateOf<Int?>(null) }

    // When a scrollToDateKey arrives, find its position in `days` and scroll there.
    LaunchedEffect(scrollToDateKey, days) {
        if (scrollToDateKey != null && days.isNotEmpty()) {
            val index = days.indexOfFirst { it.dateKey == scrollToDateKey }
            if (index >= 0) {
                listState.scrollToItem(index)
                highlightedDateKey = scrollToDateKey
                delay(2000)
                highlightedDateKey = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp)
    ) {
        // Header row: hamburger menu on the left, "HISTORY" eyebrow on the right
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
                text = "HISTORY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "By the day",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(days, key = { it.dateKey }) { day ->
                DayCard(
                    day = day,
                    isHighlighted = day.dateKey == highlightedDateKey,
                    onEditLifting = onEditLifting,
                    onEditCardio = onEditCardio,
                    onEditStretching = onEditStretching,
                    onDeleteLifting = { deleteLiftingId = it },
                    onDeleteCardio = { deleteCardioId = it },
                    onDeleteStretching = { deleteStretchingId = it },
                    onDeleteQuickLog = { deleteQuickLogDateKey = it }
                )
            }

            if (!hasAnyData) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log a workout from the Log tab to start filling in history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialogs ──
    deleteLiftingId?.let { id ->
        DeleteSessionDialog(
            sessionLabel = "weights session",
            onConfirm = {
                viewModel.deleteLiftingSession(id)
                deleteLiftingId = null
            },
            onDismiss = { deleteLiftingId = null }
        )
    }
    deleteCardioId?.let { id ->
        DeleteSessionDialog(
            sessionLabel = "cardio session",
            onConfirm = {
                viewModel.deleteCardioSession(id)
                deleteCardioId = null
            },
            onDismiss = { deleteCardioId = null }
        )
    }
    deleteStretchingId?.let { id ->
        DeleteSessionDialog(
            sessionLabel = "stretching session",
            onConfirm = {
                viewModel.deleteStretchingSession(id)
                deleteStretchingId = null
            },
            onDismiss = { deleteStretchingId = null }
        )
    }

    deleteQuickLogDateKey?.let { dateKey ->
        DeleteSessionDialog(
            sessionLabel = "quick-log marker",
            onConfirm = {
                viewModel.deleteQuickLog(dateKey)
                deleteQuickLogDateKey = null
            },
            onDismiss = { deleteQuickLogDateKey = null }
        )
    }
}

@Composable
private fun DeleteSessionDialog(
    sessionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $sessionLabel?") },
        text = { Text("This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("DELETE", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun DayCard(
    day: WorkoutDay,
    isHighlighted: Boolean = false,
    onEditLifting: (Int) -> Unit = {},
    onEditCardio: (Int) -> Unit = {},
    onEditStretching: (Int) -> Unit = {},
    onDeleteLifting: (Int) -> Unit = {},
    onDeleteCardio: (Int) -> Unit = {},
    onDeleteStretching: (Int) -> Unit = {},
    onDeleteQuickLog: (Int) -> Unit = {}
) {
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 600),
        label = "highlightBorder"
    )
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Day header — date + type badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayLabel(day.dateKey),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isToday(day.dateKey))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    // Show short date as subtitle only for TODAY/YESTERDAY labels,
                    // where the relative label hides the actual date.
                    if (isToday(day.dateKey)) {
                        Text(
                            text = formatDateKeyShort(day.dateKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Type badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (day.hasLifting) TypeBadge(Icons.Filled.FitnessCenter)
                    if (day.hasCardio) TypeBadge(Icons.Filled.DirectionsRun)
                    if (day.hasStretching) TypeBadge(Icons.Filled.SelfImprovement)
                    if (day.quickLogged) TypeBadge(Icons.Filled.Bolt)
                }
            }

            // Empty state — only when the day has NOTHING (no sessions, no quick-mark)
            if (day.isFullyEmpty) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Nothing logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            // Quick-logged section (shown for days marked via long-press, no real sessions)
            if (day.quickLogged && day.isEmpty) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK-LOGGED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onDeleteQuickLog(day.dateKey) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove mark",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "Day marked as worked out (no details).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            // Sessions stacked. First section gets a top spacer; subsequent ones get dividers.
            var isFirstSection = true
            day.lifting.forEach { entry ->
                if (isFirstSection) {
                    Spacer(modifier = Modifier.height(14.dp))
                    isFirstSection = false
                } else {
                    SessionDivider()
                }
                LiftingSection(
                    entry = entry,
                    onEdit = { onEditLifting(entry.id) },
                    onDelete = { onDeleteLifting(entry.id) }
                )
            }
            day.cardio.forEach { entry ->
                if (isFirstSection) {
                    Spacer(modifier = Modifier.height(14.dp))
                    isFirstSection = false
                } else {
                    SessionDivider()
                }
                CardioSection(
                    entry = entry,
                    onEdit = { onEditCardio(entry.id) },
                    onDelete = { onDeleteCardio(entry.id) }
                )
            }
            day.stretching.forEach { entry ->
                if (isFirstSection) {
                    Spacer(modifier = Modifier.height(14.dp))
                    isFirstSection = false
                } else {
                    SessionDivider()
                }
                StretchingSection(
                    entry = entry,
                    onEdit = { onEditStretching(entry.id) },
                    onDelete = { onDeleteStretching(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SessionDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
    Spacer(modifier = Modifier.height(12.dp))
}

/**
 * Standard "LABEL — actions" row used at the top of session sections.
 * Renders the yellow label on the left, with edit/delete icons on the right.
 */
@Composable
private fun SectionHeaderRow(
    label: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        SectionActionIcons(onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun SectionActionIcons(onEdit: () -> Unit, onDelete: () -> Unit) {
    IconButton(
        onClick = onEdit,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
    IconButton(
        onClick = onDelete,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun LiftingSection(
    entry: LiftingEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        SectionHeaderRow(
            label = "WEIGHTS",
            onEdit = onEdit,
            onDelete = onDelete
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (entry.exercises.isEmpty()) {
            Text(
                "(no exercises recorded)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        val totalSets = entry.exercises.sumOf { it.sets.size }
        val totalVolume = entry.exercises.sumOf { ex ->
            ex.sets.sumOf { set ->
                (set.reps.toIntOrNull() ?: 0) * (set.weight.toIntOrNull() ?: 0)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MiniStat("EXERCISES", "${entry.exercises.size}")
            MiniStat("SETS", "$totalSets")
            MiniStat(
                label = "VOLUME",
                value = if (totalVolume > 0) "$totalVolume" else "—",
                valueIsAccent = totalVolume > 0
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        entry.exercises.forEach { ex ->
            Text(
                text = "• ${ex.name}  (${ex.sets.size})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun CardioSection(
    entry: CardioEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        // Section header: "CARDIO" + type subtitle (e.g., "TREADMILL") + edit/delete icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "CARDIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (entry.cardioType.isNotBlank()) {
                Text(
                    text = " · ${entry.cardioType.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            SectionActionIcons(onEdit = onEdit, onDelete = onDelete)
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Session totals strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (entry.duration.isNotBlank()) MiniStat("DURATION", "${entry.duration}m", valueIsAccent = true)
            if (entry.distance.isNotBlank()) MiniStat("DISTANCE", "${entry.distance}mi")
            if (entry.calories.isNotBlank()) MiniStat("CALORIES", entry.calories)
        }

        // Phase breakdown (treadmill only)
        if (entry.phases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            entry.phases.forEach { phase ->
                Text(
                    text = "• ${phaseSummaryLine(phase)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        if (entry.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "Warm-up walk · 5 min · 3.5 mph" or similar. */
private fun phaseSummaryLine(phase: ca.bpmproperty.gymlogger.data.CardioPhase): String {
    val parts = mutableListOf<String>()
    if (phase.label.isNotBlank()) parts.add(phase.label)
    if (phase.durationMin.isNotBlank()) parts.add("${phase.durationMin} min")
    val speed = formatRange(phase.speedStart, phase.speedEnd)
    if (speed != null) parts.add("$speed mph")
    val incline = formatRange(phase.inclineStart, phase.inclineEnd)
    if (incline != null) parts.add("$incline%")
    return parts.joinToString(" · ").ifBlank { "Untitled phase" }
}

private fun formatRange(start: String, end: String): String? {
    val s = start.trim()
    val e = end.trim()
    return when {
        s.isBlank() && e.isBlank() -> null
        s.isBlank() -> e
        e.isBlank() -> s
        s == e -> s
        else -> "$s→$e"
    }
}

@Composable
private fun StretchingSection(
    entry: StretchingEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        SectionHeaderRow(
            label = "STRETCHING",
            onEdit = onEdit,
            onDelete = onDelete
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (entry.stretches.isEmpty()) {
            Text(
                "(no stretches recorded)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            MiniStat("STRETCHES", "${entry.stretches.size}", valueIsAccent = true)
            Spacer(modifier = Modifier.height(6.dp))
            entry.stretches.forEach { st ->
                val durLabel = if (st.duration.isNotBlank()) "  ${st.duration}s" else ""
                Text(
                    text = "• ${st.name}$durLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
        if (entry.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    valueIsAccent: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (valueIsAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
