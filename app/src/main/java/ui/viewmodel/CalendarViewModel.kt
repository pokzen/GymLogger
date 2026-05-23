package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.WorkoutRepository
import ca.bpmproperty.gymlogger.data.dateKeyMinusDays
import ca.bpmproperty.gymlogger.data.monthKey
import ca.bpmproperty.gymlogger.data.todayDateKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Aggregated workout-calendar data for the heatmap on the Log screen.
 *
 * - workoutDates: every dateKey with at least one session of any type
 * - currentStreak: number of consecutive days up to & including today with at least one workout
 *                  (0 if today has nothing logged)
 * - daysThisMonth: count of distinct workout days in the current calendar month
 */
data class CalendarData(
    val workoutDates: Set<Int> = emptySet(),
    val currentStreak: Int = 0,
    val daysThisMonth: Int = 0
)

class CalendarViewModel(
    repository: WorkoutRepository
) : ViewModel() {

    val data: StateFlow<CalendarData> = combine(
        repository.getAllLifting(),
        repository.getAllCardio(),
        repository.getAllStretching()
    ) { lifting, cardio, stretching ->
        val dates = buildSet<Int> {
            lifting.forEach { add(it.dateKey) }
            cardio.forEach { add(it.dateKey) }
            stretching.forEach { add(it.dateKey) }
        }
        CalendarData(
            workoutDates = dates,
            currentStreak = computeCurrentStreak(dates),
            daysThisMonth = dates.count { monthKey(it) == monthKey(todayDateKey()) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarData()
    )
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
