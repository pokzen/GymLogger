package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.CardioPhase
import ca.bpmproperty.gymlogger.data.CardioSession
import ca.bpmproperty.gymlogger.data.WorkoutRepository
import ca.bpmproperty.gymlogger.data.todayDateKey
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Type of cardio being logged. Stored as the string value to keep the schema simple. */
enum class CardioType(val storageValue: String, val label: String) {
    TREADMILL("treadmill", "Treadmill"),
    RUNNING("running", "Running"),
    CYCLING("cycling", "Cycling"),
    OTHER("other", "Other")
}

class CardioViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    /** Which cardio type the user is logging. Treadmill by default. */
    var cardioType by mutableStateOf(CardioType.TREADMILL)

    // Session-totals fields, used for all cardio types
    var duration by mutableStateOf("")
    var distance by mutableStateOf("")
    var calories by mutableStateOf("")
    var notes by mutableStateOf("")

    /** Phases (used only when cardioType == TREADMILL). */
    val phases = mutableStateListOf<CardioPhase>()

    /** The last phase-sum we used to auto-fill `duration`. Used to detect user override. */
    private var lastAppliedPhaseSum: String = ""

    /** Sum of all phase durations as a string ("" if no parseable phase durations). */
    private fun computePhaseSumDuration(): String {
        val sum = phases.sumOf { it.durationMin.toIntOrNull() ?: 0 }
        return if (sum > 0) sum.toString() else ""
    }

    /**
     * Apply auto-fill rule: if `duration` is blank or matches the previous auto-applied
     * phase sum (meaning the user hasn't overridden it), update `duration` to the new sum.
     */
    private fun reapplyDurationAutoFill() {
        val newSum = computePhaseSumDuration()
        if (duration.isBlank() || duration == lastAppliedPhaseSum) {
            duration = newSum
            lastAppliedPhaseSum = newSum
        }
    }

    /** Calendar day this session will be attributed to. Defaults to today. */
    var selectedDateKey by mutableStateOf(todayDateKey())

    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    /** When non-null, save() updates this row instead of inserting. */
    private var editingSessionId: Int? = null
    private var editingLoaded = false

    private val jsonReader = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Load an existing cardio session into this view-model for editing. */
    fun loadFromSession(sessionId: Int) {
        if (editingLoaded || sessionId <= 0) return
        editingLoaded = true
        editingSessionId = sessionId
        viewModelScope.launch {
            val session = repository.getCardio(sessionId) ?: return@launch
            cardioType = CardioType.values().firstOrNull { it.storageValue == session.cardioType } ?: CardioType.TREADMILL
            duration = session.duration
            distance = session.distance
            calories = session.calories
            notes = session.notes
            selectedDateKey = session.dateKey
            val parsedPhases: List<CardioPhase> = try {
                if (session.phases.isBlank()) emptyList()
                else jsonReader.decodeFromString(session.phases)
            } catch (_: Throwable) {
                emptyList()
            }
            phases.clear()
            phases.addAll(parsedPhases)
        }
    }

    /**
     * Save is valid if we have *some* indication a workout happened.
     * For treadmill: either phases exist OR duration is filled.
     * For other types: duration must be filled.
     */
    val isValid: Boolean
        get() = when (cardioType) {
            CardioType.TREADMILL -> phases.isNotEmpty() || duration.isNotBlank()
            else -> duration.isNotBlank()
        }

    // ---- Phase mutations ----

    fun addPhase(phase: CardioPhase) {
        phases.add(phase)
        reapplyDurationAutoFill()
    }

    fun editPhase(index: Int, phase: CardioPhase) {
        if (index !in phases.indices) return
        phases[index] = phase
        reapplyDurationAutoFill()
    }

    fun removePhase(index: Int) {
        if (index !in phases.indices) return
        phases.removeAt(index)
        reapplyDurationAutoFill()
    }

    /**
     * Computed pace in "MM:SS/mi" form. Returns null if duration or distance is missing
     * or can't be parsed, or if distance is zero.
     */
    val computedPace: String?
        get() {
            val durMin = duration.toDoubleOrNull() ?: return null
            val distMi = distance.toDoubleOrNull() ?: return null
            if (distMi <= 0.0 || durMin <= 0.0) return null
            val paceMin = durMin / distMi
            val minutes = paceMin.toInt()
            val seconds = ((paceMin - minutes) * 60).toInt()
            return "%d:%02d".format(minutes, seconds)
        }

    // ---- Save ----

    fun save(onDone: () -> Unit) {
        if (!isValid || isSaving) return
        isSaving = true
        saveError = null
        viewModelScope.launch {
            try {
                val phasesJson = if (cardioType == CardioType.TREADMILL && phases.isNotEmpty()) {
                    Json.encodeToString(phases.toList())
                } else {
                    ""
                }
                val id = editingSessionId
                if (id != null) {
                    val existing = repository.getCardio(id)
                    if (existing != null) {
                        repository.updateCardio(
                            existing.copy(
                                dateKey = selectedDateKey,
                                cardioType = cardioType.storageValue,
                                duration = duration,
                                distance = distance,
                                calories = calories,
                                phases = phasesJson,
                                notes = notes
                            )
                        )
                    }
                } else {
                    repository.insertCardio(
                        CardioSession(
                            dateKey = selectedDateKey,
                            cardioType = cardioType.storageValue,
                            duration = duration,
                            distance = distance,
                            calories = calories,
                            phases = phasesJson,
                            notes = notes
                        )
                    )
                }
                onDone()
            } catch (t: Throwable) {
                saveError = t.message ?: "Failed to save session"
            } finally {
                isSaving = false
            }
        }
    }

    fun clearError() {
        saveError = null
    }
}
