package ca.bpmproperty.gymlogger.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.data.buildCalendarGrid
import ca.bpmproperty.gymlogger.data.isToday
import ca.bpmproperty.gymlogger.data.monthLabel
import ca.bpmproperty.gymlogger.data.todayDateKey

/**
 * GitHub-style horizontal heatmap of workout days.
 *
 * - 7 rows (Mon top → Sun bottom)
 * - [weeksBack] columns; rightmost = current week (today)
 * - Filled cells (yellow) = a workout was logged that day
 * - Empty cells (dark) = no workout
 * - Today's cell has a subtle yellow border
 * - Future cells (after today in the current week) are non-interactive and dimmed
 *
 * Auto-scrolls to the end on first composition so today is visible.
 */
@Composable
fun WorkoutCalendar(
    workoutDates: Set<Int>,
    onDayTap: (dateKey: Int, hasWorkout: Boolean) -> Unit,
    onDayLongPress: (dateKey: Int, hasWorkout: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    weeksBack: Int = 12
) {
    val grid = remember(weeksBack) { buildCalendarGrid(weeksBack) }
    val today = remember { todayDateKey() }
    val listState = rememberLazyListState()

    // Auto-scroll to end on first composition so the most recent week is on screen.
    LaunchedEffect(grid.size) {
        if (grid.isNotEmpty()) {
            listState.scrollToItem(grid.lastIndex)
        }
    }

    Row(modifier = modifier) {
        // Day-of-week labels on the left
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 6.dp, top = 18.dp) // top offset for month label row
        ) {
            DayLabel("M")
            DayLabel("T")
            DayLabel("W")
            DayLabel("T")
            DayLabel("F")
            DayLabel("S")
            DayLabel("S")
        }

        // Scrollable week columns
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(
                count = grid.size,
                key = { index -> grid[index].first() } // Monday of week as stable key
            ) { weekIndex ->
                val week = grid[weekIndex]
                // Determine if a month label should be shown above this column:
                // show it when the week contains the 1st of any month, or it's the leftmost column.
                val monthLabelText: String? = monthLabelForWeek(week, weekIndex)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Month label slot (always reserves height for alignment)
                    Box(
                        modifier = Modifier.height(14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (monthLabelText != null) {
                            Text(
                                text = monthLabelText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    week.forEach { dateKey ->
                        DayCell(
                            dateKey = dateKey,
                            hasWorkout = dateKey in workoutDates,
                            isToday = isToday(dateKey),
                            isFuture = dateKey > today,
                            onTap = { hasWorkout -> onDayTap(dateKey, hasWorkout) },
                            onLongPress = { hasWorkout -> onDayLongPress(dateKey, hasWorkout) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayLabel(letter: String) {
    Box(
        modifier = Modifier.size(width = 12.dp, height = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    dateKey: Int,
    hasWorkout: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onTap: (hasWorkout: Boolean) -> Unit,
    onLongPress: (hasWorkout: Boolean) -> Unit
) {
    val shape = RoundedCornerShape(3.dp)
    val baseColor = when {
        isFuture -> Color.Transparent
        hasWorkout -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val cellModifier = Modifier
        .size(16.dp)
        .clip(shape)
        .background(baseColor)
        .then(
            if (isToday)
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
            else
                Modifier
        )
        .then(
            if (!isFuture)
                Modifier.combinedClickable(
                    onClick = { onTap(hasWorkout) },
                    onLongClick = { onLongPress(hasWorkout) }
                )
            else
                Modifier
        )

    Box(modifier = cellModifier)
}

/** Show a month label above the leftmost column or any week containing the 1st of a month. */
private fun monthLabelForWeek(week: List<Int>, weekIndex: Int): String? {
    // Always label the leftmost column
    if (weekIndex == 0) return monthLabel(week.first())
    // Otherwise label weeks that contain the 1st of a month
    val firstOfMonth = week.firstOrNull { it % 100 == 1 }
    return firstOfMonth?.let { monthLabel(it) }
}
