package ca.bpmproperty.gymlogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.bpmproperty.gymlogger.data.dateKeyMinusDays
import ca.bpmproperty.gymlogger.data.dayOfWeekMondayBased
import ca.bpmproperty.gymlogger.data.isToday
import ca.bpmproperty.gymlogger.data.todayDateKey
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

/**
 * Compact inline week view: one row showing the current calendar week
 * (Monday → Sunday). Day-of-week letters across the top, date numbers below,
 * a bold yellow check mark drawn over any day with a workout, and a filled
 * circle behind the date number for today.
 *
 * The whole strip is one tap target — tapping anywhere opens the full
 * scrollable calendar popout via [onOpenCalendar].
 */
@Composable
fun WorkoutWeekStrip(
    workoutDates: Set<Int>,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { todayDateKey() }
    // Build the seven dateKeys for the current Mon–Sun week containing today.
    val weekDates = remember(today) {
        val todayDow = dayOfWeekMondayBased(today) // 0 = Mon ... 6 = Sun
        (0..6).map { dow -> dateKeyMinusDays(today, todayDow - dow) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenCalendar),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDates.forEach { dateKey ->
            DayColumn(
                dateKey = dateKey,
                hasWorkout = dateKey in workoutDates,
                isToday = isToday(dateKey)
            )
        }
    }
}

@Composable
private fun DayColumn(
    dateKey: Int,
    hasWorkout: Boolean,
    isToday: Boolean
) {
    val dayOfMonth = dateKey % 100
    val dayOfWeekIdx = remember(dateKey) { dayOfWeekMondayBased(dateKey) }
    val dayLetter = listOf("M", "T", "W", "T", "F", "S", "S")[dayOfWeekIdx]

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = dayLetter,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Date number — wrapped in a circle background if today.
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Big yellow check mark for workout days. Drawn with Canvas so it's
        // chunky and matches the screenshot style — not a thin icon.
        Box(
            modifier = Modifier.size(width = 36.dp, height = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasWorkout) {
                CheckMark(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Bold V-shaped check mark drawn at the size of its parent. */
@Composable
private fun CheckMark(color: Color) {
    Canvas(modifier = Modifier.size(width = 28.dp, height = 22.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = h * 0.18f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        // Three-point V: left baseline → bottom vertex → upper right.
        val left = Offset(w * 0.08f, h * 0.45f)
        val bottom = Offset(w * 0.42f, h * 0.92f)
        val right = Offset(w * 0.95f, h * 0.05f)
        drawLine(color, left, bottom, strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, bottom, right, strokeWidth = stroke.width, cap = stroke.cap)
    }
}
