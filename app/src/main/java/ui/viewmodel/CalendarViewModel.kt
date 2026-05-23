package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.QuickLog
import ca.bpmproperty.gymlogger.data.WorkoutRepository
import ca.bpmproperty.gymlogger.data.dateKeyMinusDays
import ca.bpmproperty.gymlogger.data.todayDateKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Aggregated workout-calendar data for the Log screen.
 *
 * - workoutDates: every dateKey with at least one session of any type OR a quick-log marker
 * - currentStreak: number of consecutive days up to & including today with at least one workout
 *                  (0 if today has nothing logged)
 * - daysInLast30: count of distinct workout days within the rolling 30-day window
 *                 ending today (inclusive). Rolling window — not a calendar month.
 */
data class CalendarData(
    val workoutDates: Set<Int> = emptySet(),
    val currentStreak: Int = 0,
    val daysInLast30: Int = 0
)

class CalendarViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val data: StateFlow<CalendarData> = combine(
        repository.getAllLifting(),
        repository.getAllCardio(),
        repository.getAllStretching(),
        repository.getAllQuickLogs()
    ) { lifting, cardio, stretching, quickLogs ->
        val dates = buildSet<Int> {
            lifting.forEach { add(it.dateKey) }
            cardio.forEach { add(it.dateKey) }
            stretching.forEach { add(it.dateKey) }
            quickLogs.forEach { add(it.dateKey) }
        }
        val today = todayDateKey()
        // 30-day inclusive window: today, plus the 29 days before it.
        val windowStart = dateKeyMinusDays(today, 29)
        CalendarData(
            workoutDates = dates,
            currentStreak = computeCurrentStreak(dates),
            daysInLast30 = dates.count { it in windowStart..today }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarData()
    )

    /** Mark a past or today's day as worked-out, no details. Future dates are rejected. */
    fun markDayQuick(dateKey: Int) {
        if (dateKey > todayDateKey()) return
        viewModelScope.launch {
            repository.upsertQuickLog(QuickLog(dateKey = dateKey))
        }
    }

    /** Remove a quick-log marker for the given day. */
    fun unmarkDayQuick(dateKey: Int) {
        viewModelScope.launch {
            repository.deleteQuickLog(dateKey)
        }
    }
}

/** Walk backwards from today; stop on the first day with no workout. */
private fun computeCurrentStreak(workoutDates: Set<Int>): Int {
    var streak = 0
    var cursor = todayDateKey()
    while (cursor in workoutDates) {
        streak += 1
        cursor = dateKeyMinusDays(cursor, 1)
    }
    return streak
}
