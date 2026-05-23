package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.CardioDraftPayload
import ca.bpmproperty.gymlogger.data.CardioPhase
import ca.bpmproperty.gymlogger.data.CardioSession
import ca.bpmproperty.gymlogger.data.SessionDraft
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

    // Backed properties: every external write auto-persists to the draft table.

    private var _cardioType by mutableStateOf(CardioType.TREADMILL)
    var cardioType: CardioType
        get() = _cardioType
        set(value) { _cardioType = value; wasRestoredFromDraft = false; persistDraft() }

    private var _duration by mutableStateOf("")
    var duration: String
        get() = _duration
        set(value) { _duration = value; wasRestoredFromDraft = false; persistDraft() }

    private var _distance by mutableStateOf("")
    var distance: String
        get() = _distance
        set(value) { _distance = value; wasRestoredFromDraft = false; persistDraft() }

    private var _calories by mutableStateOf("")
    var calories: String
        get() = _calories
        set(value) { _calories = value; wasRestoredFromDraft = false; persistDraft() }

    private var _notes by mutableStateOf("")
    var notes: String
        get() = _notes
        set(value) { _notes = value; wasRestoredFromDraft = false; persistDraft() }

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
     * Writes directly to `_duration` to avoid the public setter firing persistDraft mid-call.
     */
    private fun reapplyDurationAutoFill() {
        val newSum = computePhaseSumDuration()
        if (_duration.isBlank() || _duration == lastAppliedPhaseSum) {
            _duration = newSum
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

    /** True once we've attempted to load a draft on init. */
    private var draftChecked = false

    /** True while a restored draft is intact and the user hasn't yet made changes. */
    var wasRestoredFromDraft by mutableStateOf(false)
        private set

    private val jsonReader = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Restore an in-progress cardio session from the draft table, if any exists. */
    fun loadDraftIfAny() {
        if (draftChecked || editingSessionId != null) return
        draftChecked = true
        viewModelScope.launch {
            val draft = repository.getDraft(DRAFT_TYPE) ?: return@launch
            val payload: CardioDraftPayload = try {
                jsonReader.decodeFromString(draft.payloadJson)
            } catch (_: Throwable) {
                return@launch
            }
            _cardioType = CardioType.values().firstOrNull { it.storageValue == payload.cardioType } ?: CardioType.TREADMILL
            _duration = payload.duration
            _distance = payload.distance
            _calories = payload.calories
            _notes = payload.notes
            phases.clear()
            phases.addAll(payload.phases)
            selectedDateKey = draft.dateKey
            // Only signal the banner if the draft actually had content
            val hasContent = payload.phases.isNotEmpty() ||
                payload.duration.isNotBlank() ||
                payload.distance.isNotBlank() ||
                payload.calories.isNotBlank() ||
                payload.notes.isNotBlank()
            if (hasContent) wasRestoredFromDraft = true
        }
    }

    /** Wipe the in-progress cardio session and the persisted draft row. */
    fun discardDraft() {
        _cardioType = CardioType.TREADMILL
        _duration = ""
        _distance = ""
        _calories = ""
        _notes = ""
        phases.clear()
        selectedDateKey = todayDateKey()
        lastAppliedPhaseSum = ""
        wasRestoredFromDraft = false
        viewModelScope.launch {
            clearDraft()
        }
    }

    /** Snapshot current state to the draft table. Skipped in edit mode. */
    private fun persistDraft() {
        if (editingSessionId != null) return
        viewModelScope.launch {
            val payload = CardioDraftPayload(
                cardioType = _cardioType.storageValue,
                duration = _duration,
                distance = _distance,
                calories = _calories,
                notes = _notes,
                phases = phases.toList()
            )
            val json = Json.encodeToString(payload)
            repository.upsertDraft(
                SessionDraft(
                    sessionType = DRAFT_TYPE,
                    dateKey = selectedDateKey,
                    payloadJson = json
                )
            )
        }
    }

    private suspend fun clearDraft() {
        repository.deleteDraft(DRAFT_TYPE)
    }

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
        wasRestoredFromDraft = false
        persistDraft()
    }

    fun editPhase(index: Int, phase: CardioPhase) {
        if (index !in phases.indices) return
        phases[index] = phase
        reapplyDurationAutoFill()
        wasRestoredFromDraft = false
        persistDraft()
    }

    fun removePhase(index: Int) {
        if (index !in phases.indices) return
        phases.removeAt(index)
        reapplyDurationAutoFill()
        wasRestoredFromDraft = false
        persistDraft()
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
                clearDraft()
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

    companion object {
        private const val DRAFT_TYPE = "cardio"
    }
}
