package ca.sb.gymlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.sb.gymlogger.data.ExerciseEntry
import ca.sb.gymlogger.data.LiftingSession
import ca.sb.gymlogger.data.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

/**
 * Aggregate stats across all logged sessions.
 *
 * - totalWorkouts: count of all sessions (lifting + cardio + stretching)
 * - totalSets: sum of sets across all lifting sessions
 * - totalVolume: sum of reps × weight across all lifting sets (in lbs)
 */
data class SummaryStats(
    val totalWorkouts: Int = 0,
    val totalWorkoutDays: Int = 0,
    val totalSets: Int = 0,
    val totalVolume: Int = 0,
    val liftingCount: Int = 0,
    val cardioCount: Int = 0,
    val stretchingCount: Int = 0
)

class SummaryViewModel(
    repository: WorkoutRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val stats: StateFlow<SummaryStats> = combine(
        repository.getAllLifting(),
        repository.getAllCardio(),
        repository.getAllStretching()
    ) { lifting, cardio, stretching ->
        var totalSets = 0
        var totalVolume = 0
        lifting.forEach { session ->
            val exercises = parseLifting(session)
            exercises.forEach { ex ->
                totalSets += ex.sets.size
                ex.sets.forEach { set ->
                    val r = set.reps.toIntOrNull() ?: 0
                    val w = set.weight.toIntOrNull() ?: 0
                    totalVolume += r * w
                }
            }
        }
        val distinctDays = buildSet<Int> {
            lifting.forEach { add(it.dateKey) }
            cardio.forEach { add(it.dateKey) }
            stretching.forEach { add(it.dateKey) }
        }.size

        SummaryStats(
            totalWorkouts = lifting.size + cardio.size + stretching.size,
            totalWorkoutDays = distinctDays,
            totalSets = totalSets,
            totalVolume = totalVolume,
            liftingCount = lifting.size,
            cardioCount = cardio.size,
            stretchingCount = stretching.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryStats()
    )

    private fun parseLifting(session: LiftingSession): List<ExerciseEntry> =
        try {
            json.decodeFromString(session.exercises)
        } catch (_: Throwable) {
            emptyList()
        }
}
