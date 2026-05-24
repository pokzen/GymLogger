package ca.sb.gymlogger.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.sb.gymlogger.data.SessionDraft
import ca.sb.gymlogger.data.StretchEntry
import ca.sb.gymlogger.data.StretchingDraftPayload
import ca.sb.gymlogger.data.StretchingSession
import ca.sb.gymlogger.data.WorkoutRepository
import ca.sb.gymlogger.data.todayDateKey
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StretchingViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val stretches = mutableStateListOf<StretchEntry>()

    private var _notes by mutableStateOf("")
    var notes: String
        get() = _notes
        set(value) { _notes = value; wasRestoredFromDraft = false; persistDraft() }

    /** Calendar day this session will be attributed to. Defaults to today. */
    var selectedDateKey by mutableStateOf(todayDateKey())

    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    private var editingSessionId: Int? = null
    private var editingLoaded = false
    private var draftChecked = false

    /** True while a restored draft is intact and the user hasn't yet made changes. */
    var wasRestoredFromDraft by mutableStateOf(false)
        private set

    private val jsonReader = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Restore an in-progress stretching session from the draft table, if any exists. */
    fun loadDraftIfAny() {
        if (draftChecked || editingSessionId != null) return
        draftChecked = true
        viewModelScope.launch {
            val draft = repository.getDraft(DRAFT_TYPE) ?: return@launch
            val payload: StretchingDraftPayload = try {
                jsonReader.decodeFromString(draft.payloadJson)
            } catch (_: Throwable) {
                return@launch
            }
            stretches.clear()
            stretches.addAll(payload.stretches)
            _notes = payload.notes
            selectedDateKey = draft.dateKey
            if (payload.stretches.isNotEmpty() || payload.notes.isNotBlank()) {
                wasRestoredFromDraft = true
            }
        }
    }

    /** Wipe the in-progress stretching session and the persisted draft row. */
    fun discardDraft() {
        stretches.clear()
        _notes = ""
        selectedDateKey = todayDateKey()
        wasRestoredFromDraft = false
        viewModelScope.launch {
            clearDraft()
        }
    }

    private fun persistDraft() {
        if (editingSessionId != null) return
        viewModelScope.launch {
            val payload = StretchingDraftPayload(
                stretches = stretches.toList(),
                notes = _notes
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

    /** Load an existing stretching session into this view-model for editing. */
    fun loadFromSession(sessionId: Int) {
        if (editingLoaded || sessionId <= 0) return
        editingLoaded = true
        editingSessionId = sessionId
        viewModelScope.launch {
            val session = repository.getStretching(sessionId) ?: return@launch
            val parsed: List<StretchEntry> = try {
                if (session.stretches.isBlank()) emptyList()
                else jsonReader.decodeFromString(session.stretches)
            } catch (_: Throwable) {
                emptyList()
            }
            stretches.clear()
            stretches.addAll(parsed)
            _notes = session.notes
            selectedDateKey = session.dateKey
        }
    }

    fun addStretch(name: String, duration: String) {
        stretches.add(StretchEntry(name, duration))
        wasRestoredFromDraft = false
        persistDraft()
    }

    /** Replace a specific stretch in place. No-op if index is invalid. */
    fun editStretch(index: Int, name: String, duration: String) {
        if (index !in stretches.indices) return
        stretches[index] = StretchEntry(name, duration)
        wasRestoredFromDraft = false
        persistDraft()
    }

    fun removeStretch(index: Int) {
        if (index !in stretches.indices) return
        stretches.removeAt(index)
        wasRestoredFromDraft = false
        persistDraft()
    }

    /** Persist the current session. Calls [onDone] when the database write completes. */
    fun save(onDone: () -> Unit) {
        if (stretches.isEmpty() || isSaving) return
        isSaving = true
        saveError = null
        viewModelScope.launch {
            try {
                val json = Json.encodeToString(stretches.toList())
                val id = editingSessionId
                if (id != null) {
                    val existing = repository.getStretching(id)
                    if (existing != null) {
                        repository.updateStretching(
                            existing.copy(
                                dateKey = selectedDateKey,
                                stretches = json,
                                notes = notes
                            )
                        )
                    }
                } else {
                    repository.insertStretching(
                        StretchingSession(
                            dateKey = selectedDateKey,
                            stretches = json,
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
        private const val DRAFT_TYPE = "stretching"
    }
}
