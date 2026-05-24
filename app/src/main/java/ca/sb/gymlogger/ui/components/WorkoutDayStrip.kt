package ca.sb.gymlogger.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.sb.gymlogger.data.dateKeyMinusDays
import ca.sb.gymlogger.data.isToday
import ca.sb.gymlogger.data.monthLabel
import ca.sb.gymlogger.data.todayDateKey

/**
 * Horizontal day strip showing the last [daysBack] days ending with today.
 *
 * Each cell is a tap-friendly square with the day-of-month number. Workout days
 * are filled with the primary colour; non-workout days are dim. Today carries a
 * primary-colour border. A small month label sits above the cell whenever the
 * day-of-month is 1 (or for the leftmost cell), so the user can orient when
 * scrolling backwards across a month boundary.
 *
 * Tap → [onDayTap], long-press → [onDayLongPress]. Same contract as the older
 * WorkoutCalendar component so callers swap in without further wiring.
 *
 * Auto-scrolls to the end (today) on first composition.
 */
@Composable
fun WorkoutDayStrip(
    workoutDates: Set<Int>,
    onDayTap: (dateKey: Int, hasWorkout: Boolean) -> Unit,
    onDayLongPress: (dateKey: Int, hasWorkout: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    daysBack: Int = 30
) {
    val today = remember { todayDateKey() }
    // Oldest -> newest, so newest (today) ends on the right.
    val dates = remember(today, daysBack) {
        (daysBack - 1 downTo 0).map { offset -> dateKeyMinusDays(today, offset) }
    }
    val listState = rememberLazyListState()

    // Snap to today on first composition.
    LaunchedEffect(dates.size) {
        if (dates.isNotEmpty()) {
            listState.scrollToItem(dates.lastIndex)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        items(
            count = dates.size,
            key = { idx -> dates[idx] }
        ) { idx ->
            val dateKey = dates[idx]
            val dayOfMonth = dateKey % 100
            // Show a month label above the leftmost column or above day 1.
            val showMonthLabel = idx == 0 || dayOfMonth == 1
            DayCell(
                dateKey = dateKey,
                dayOfMonth = dayOfMonth,
                hasWorkout = dateKey in workoutDates,
                isToday = isToday(dateKey),
                monthLabel = if (showMonthLabel) monthLabel(dateKey) else null,
                onTap = { hasWorkout -> onDayTap(dateKey, hasWorkout) },
                onLongPress = { hasWorkout -> onDayLongPress(dateKey, hasWorkout) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    dateKey: Int,
    dayOfMonth: Int,
    hasWorkout: Boolean,
    isToday: Boolean,
    monthLabel: String?,
    onTap: (hasWorkout: Boolean) -> Unit,
    onLongPress: (hasWorkout: Boolean) -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val cellBg = if (hasWorkout)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant
    val numberColor = if (hasWorkout)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Reserve space for the month label so cells with/without one stay aligned.
        Box(
            modifier = Modifier.height(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (monthLabel != null) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 48.dp)
                .clip(shape)
                .background(cellBg)
                .then(
                    if (isToday) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        shape
                    ) else Modifier
                )
                .combinedClickable(
                    onClick = { onTap(hasWorkout) },
                    onLongClick = { onLongPress(hasWorkout) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = numberColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
