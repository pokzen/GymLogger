package ca.sb.gymlogger.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.navigation.NavController
import ca.sb.gymlogger.data.dateKeyMinusDays
import ca.sb.gymlogger.data.dayOfWeekMondayBased
import ca.sb.gymlogger.data.isToday
import ca.sb.gymlogger.data.todayDateKey
import ca.sb.gymlogger.ui.components.LogOnDateSheet
import ca.sb.gymlogger.ui.components.WorkoutType
import ca.sb.gymlogger.ui.viewmodel.CalendarViewModel
import ca.sb.gymlogger.ui.viewmodel.workoutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Full-screen continuous calendar. Renders ~24 months of past data as a single
 * unbroken stream of Monday–Sunday week rows that flow across month boundaries
 * (so May 1 sits in the same row as the trailing days of April).
 *
 * A sticky month/year header sits above the grid and updates live as the user
 * scrolls — derived from the dominant month of the topmost visible week row.
 *
 * Auto-snaps to the current week on open. Tap a day to log on that date, or
 * long-press an empty day to quick-mark it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCalendarPopout(
    navController: NavController,
    onBack: () -> Unit
) {
    val calendarVM: CalendarViewModel = workoutViewModel { CalendarViewModel(it) }
    val calData by calendarVM.data.collectAsState()
    val today = remember { todayDateKey() }

    // Generate weeks: 24 months back, 3 months forward, ordered oldest -> newest.
    // Forward months give the user space to scroll past today and also help the
    // "current month" header line up correctly (since today isn't pinned to the
    // bottom edge of the scroll range any more).
    val weeks = remember(today) {
        buildContinuousWeeks(today, monthsBack = 24, monthsForward = 3)
    }

    var sheetForDate by remember { mutableStateOf<Int?>(null) }
    var quickMarkConfirmDate by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Index of the week containing today — used as the auto-scroll target and
    // as the anchor for "current" centring.
    val todayWeekIndex = remember(weeks, today) {
        weeks.indexOfFirst { week -> today in week }.coerceAtLeast(0)
    }

    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) {
            // Position today's week near the middle of the viewport, not the bottom.
            scope.launch {
                listState.scrollToItem(
                    index = (todayWeekIndex - 2).coerceAtLeast(0)
                )
            }
        }
    }

    // Sticky-header label: pick the month of the MIDDLE visible week's Thursday
    // (Thursday = column index 3, which always sits in the month that owns most
    // of the week). Using the middle index instead of the topmost means the
    // header reflects what's dominating the screen, not a sliver at the top.
    val visibleMonthLabel by remember {
        derivedStateOf {
            if (weeks.isEmpty()) {
                ""
            } else {
                val visibleInfo = listState.layoutInfo.visibleItemsInfo
                val midIndex = if (visibleInfo.isNotEmpty()) {
                    visibleInfo[visibleInfo.size / 2].index
                } else {
                    listState.firstVisibleItemIndex
                }
                val clamped = midIndex.coerceIn(0, weeks.lastIndex)
                formatMonthYearHeader(weeks[clamped][3])
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CALENDAR",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // Sticky month/year label.
            Text(
                text = visibleMonthLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
            )

            // Day-of-week header row (sticky — sits above the scrolling grid).
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { letter ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // The continuous grid.
            val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            // Faint white-ish accent at the bottom of every cell.
            val cellBottomLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(weeks.size) { weekIndex ->
                    val week = weeks[weekIndex]
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEachIndexed { colIndex, dateKey ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .then(
                                        if (colIndex > 0) Modifier.drawLeftBorder(gridLineColor)
                                        else Modifier
                                    )
                                    .then(
                                        if (weekIndex > 0) Modifier.drawTopBorder(gridLineColor)
                                        else Modifier
                                    )
                                    // Hairline white-ish accent along the bottom edge of every cell.
                                    .drawBottomHairline(cellBottomLineColor),
                                contentAlignment = Alignment.Center
                            ) {
                                DayCell(
                                    dateKey = dateKey,
                                    hasWorkout = dateKey in calData.workoutDates,
                                    isToday = isToday(dateKey),
                                    onTap = {
                                        val hasWorkout = dateKey in calData.workoutDates
                                        if (hasWorkout) {
                                            navController.navigate("history") {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                            navController.getBackStackEntry("history")
                                                .savedStateHandle["scrollToDateKey"] = dateKey
                                        } else {
                                            sheetForDate = dateKey
                                        }
                                    },
                                    onLongPress = {
                                        val hasWorkout = dateKey in calData.workoutDates
                                        if (!hasWorkout) quickMarkConfirmDate = dateKey
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Log-on-date sheet
    sheetForDate?.let { dateKey ->
        LogOnDateSheet(
            dateKey = dateKey,
            onDismiss = { sheetForDate = null },
            onTypeSelected = { type ->
                sheetForDate = null
                when (type) {
                    WorkoutType.WEIGHTS -> navController.navigate("lifting?date=$dateKey")
                    WorkoutType.CARDIO -> navController.navigate("cardio?date=$dateKey")
                    WorkoutType.STRETCHING -> navController.navigate("stretching?date=$dateKey")
                }
            }
        )
    }

    // Quick-mark confirmation
    quickMarkConfirmDate?.let { dateKey ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { quickMarkConfirmDate = null },
            title = { Text("Mark as worked out?") },
            text = {
                Text(
                    "This will flag ${ca.sb.gymlogger.data.dayLabel(dateKey)} on the calendar without logging any details. " +
                        "You can remove the mark later from History."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    calendarVM.markDayQuick(dateKey)
                    quickMarkConfirmDate = null
                }) {
                    Text("MARK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { quickMarkConfirmDate = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    dateKey: Int,
    hasWorkout: Boolean,
    isToday: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val dayOfMonth = dateKey % 100
    Column(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier.size(width = 28.dp, height = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasWorkout) {
                CheckMark(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CheckMark(color: Color) {
    Canvas(modifier = Modifier.size(width = 28.dp, height = 22.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = h * 0.20f
        val left = Offset(w * 0.08f, h * 0.45f)
        val bottom = Offset(w * 0.42f, h * 0.92f)
        val right = Offset(w * 0.95f, h * 0.05f)
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        drawLine(color, left, bottom, strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color, bottom, right, strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// ── Gridline helpers ─────────────────────────────────────────────────────────

private fun Modifier.drawLeftBorder(color: Color) = this.drawBehind {
    val px = 1.dp.toPx()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = px
    )
}

private fun Modifier.drawTopBorder(color: Color) = this.drawBehind {
    val px = 1.dp.toPx()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = px
    )
}

/** Sub-pixel-thin hairline along the bottom edge of the cell. */
private fun Modifier.drawBottomHairline(color: Color) = this.drawBehind {
    val px = 0.5.dp.toPx()
    val y = size.height - px / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = px
    )
}

// ── Date helpers ─────────────────────────────────────────────────────────────

/**
 * Build a continuous list of Monday–Sunday weeks spanning [monthsBack] months
 * of history plus [monthsForward] months ahead, ordered oldest-first.
 *
 * Each week is a list of 7 dateKeys — no nulls; trailing/leading days from
 * adjacent months are real dateKeys, so the grid reads as a continuous stream.
 */
private fun buildContinuousWeeks(
    today: Int,
    monthsBack: Int,
    monthsForward: Int
): List<List<Int>> {
    val ty = today / 10000
    val tm = (today / 100) % 100

    // Start: first day of the (monthsBack-1) month ago, then walk back to its Monday.
    val startCal = Calendar.getInstance().apply {
        set(ty, tm - 1, 1)
        add(Calendar.MONTH, -(monthsBack - 1))
    }
    val firstDayDateKey = startCal.get(Calendar.YEAR) * 10000 +
        (startCal.get(Calendar.MONTH) + 1) * 100 +
        startCal.get(Calendar.DAY_OF_MONTH)
    val firstDayDow = dayOfWeekMondayBased(firstDayDateKey)
    val startMonday = dateKeyMinusDays(firstDayDateKey, firstDayDow)

    // End: last day of the month that is monthsForward months from today,
    // then walk forward to its Sunday so the final week is complete.
    val endCal = Calendar.getInstance().apply {
        set(ty, tm - 1, 1)
        add(Calendar.MONTH, monthsForward)
        // Move to last day of THAT month.
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    val lastDayDateKey = endCal.get(Calendar.YEAR) * 10000 +
        (endCal.get(Calendar.MONTH) + 1) * 100 +
        endCal.get(Calendar.DAY_OF_MONTH)
    val lastDayDow = dayOfWeekMondayBased(lastDayDateKey)
    val endSunday = dateKeyMinusDays(lastDayDateKey, lastDayDow - 6)

    // Walk week by week.
    val weeks = mutableListOf<List<Int>>()
    var monday = startMonday
    while (true) {
        val week = (0..6).map { offset -> dateKeyMinusDays(monday, -offset) }
        weeks.add(week)
        if (week.last() >= endSunday) break
        monday = dateKeyMinusDays(monday, -7)
    }
    return weeks
}

/** "MAY 2026" style heading. */
private fun formatMonthYearHeader(dateKey: Int): String {
    val cal = Calendar.getInstance().apply {
        set(dateKey / 10000, (dateKey / 100) % 100 - 1, dateKey % 100)
    }
    val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return fmt.format(Date(cal.timeInMillis)).uppercase()
}
