package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.CardioPhase
import ca.bpmproperty.gymlogger.data.CardioSession
import ca.bpmproperty.gymlogger.data.ExerciseEntry
import ca.bpmproperty.gymlogger.data.LiftingSession
import ca.bpmproperty.gymlogger.data.StretchEntry
import ca.bpmproperty.gymlogger.data.StretchingSession
import ca.bpmproperty.gymlogger.data.WorkoutRepository
import ca.bpmproperty.gymlogger.data.todayDateKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** Parsed lifting session for display. */
data class LiftingEntry(
    val id: Int,
    val exercises: List<ExerciseEntry>
)

/** Parsed cardio session for display (phases parsed out of the JSON string). */
data class CardioEntry(
    val id: Int,
    val cardioType: String,
    val duration: String,
    val distance: String,
    val calories: String,
    val phases: List<CardioPhase>,
    val notes: String
)

/** Parsed stretching session for display. */
data class StretchingEntry(
    val id: Int,
    val stretches: List<StretchEntry>,
    val notes: String
)

/** All sessions logged on a single calendar day. */
data class WorkoutDay(
    val dateKey: Int,
    val lifting: List<LiftingEntry>,
    val cardio: List<CardioEntry>,
    val stretching: List<StretchingEntry>
) {
    val isEmpty: Boolean
        get() = lifting.isEmpty() && cardio.isEmpty() && stretching.isEmpty()

    val totalSessions: Int
        get() = lifting.size + cardio.size + stretching.size

    val hasLifting: Boolean get() = lifting.isNotEmpty()
    val hasCardio: Boolean get() = cardio.isNotEmpty()
    val hasStretching: Boolean get() = stretching.isNotEmpty()
}

class HistoryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    /** Delete a lifting session by id. Idempotent — silently no-ops if the row is gone. */
    fun deleteLiftingSession(id: Int) {
        viewModelScope.launch {
            val existing = repository.getLifting(id) ?: return@launch
            repository.deleteLifting(existing)
        }
    }

    /** Delete a cardio session by id. */
    fun deleteCardioSession(id: Int) {
        viewModelScope.launch {
            val existing = repository.getCardio(id) ?: return@launch
            repository.deleteCardio(existing)
        }
    }

    /** Delete a stretching session by id. */
    fun deleteStretchingSession(id: Int) {
        viewModelScope.launch {
            val existing = repository.getStretching(id) ?: return@launch
            repository.deleteStretching(existing)
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * All workout days sorted descending (most recent first).
     * Today is always included even if empty — the screen renders it as the "TODAY" card.
     */
    val days: StateFlow<List<WorkoutDay>> = combine(
        repository.getAllLifting(),
        repository.getAllCardio(),
        repository.getAllStretching()
    ) { lifting, cardio, stretching ->
        // Collect every dateKey that has at least one session
        val keys = buildSet<Int> {
            lifting.forEach { add(it.dateKey) }
            cardio.forEach { add(it.dateKey) }
            stretching.forEach { add(it.dateKey) }
            // Always include today so the screen can render a TODAY card
            add(todayDateKey())
        }

        // Build a WorkoutDay for each
        keys.sortedDescending().map { dateKey ->
            WorkoutDay(
                dateKey = dateKey,
                lifting = lifting.filter { it.dateKey == dateKey }.map { it.toEntry(json) },
                cardio = cardio.filter { it.dateKey == dateKey }.map { it.toEntry(json) },
                stretching = stretching.filter { it.dateKey == dateKey }.map { it.toEntry(json) }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}

private fun LiftingSession.toEntry(json: Json): LiftingEntry {
    val parsed: List<ExerciseEntry> = try {
        json.decodeFromString(exercises)
    } catch (_: Throwable) {
        emptyList()
    }
    return LiftingEntry(id = id, exercises = parsed)
}

private fun CardioSession.toEntry(json: Json): CardioEntry {
    val parsedPhases: List<CardioPhase> = try {
        if (phases.isBlank()) emptyList() else json.decodeFromString(phases)
    } catch (_: Throwable) {
        emptyList()
    }
    return CardioEntry(
        id = id,
        cardioType = cardioType,
        duration = duration,
        distance = distance,
        calories = calories,
        phases = parsedPhases,
        notes = notes
    )
}

private fun StretchingSession.toEntry(json: Json): StretchingEntry {
    val parsed: List<StretchEntry> = try {
        json.decodeFromString(stretches)
    } catch (_: Throwable) {
        emptyList()
    }
    return StretchingEntry(id = id, stretches = parsed, notes = notes)
}
