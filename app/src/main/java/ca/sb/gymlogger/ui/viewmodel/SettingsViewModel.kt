package ca.sb.gymlogger.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.sb.gymlogger.data.AppPreferences
import ca.sb.gymlogger.data.WorkoutRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: WorkoutRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    /** Mirror of the persisted default cardio type so the UI dropdown updates immediately. */
    var defaultCardioType by mutableStateOf(preferences.defaultCardioType)
        private set

    /** Mirror of the persisted log name. */
    var logName by mutableStateOf(preferences.logName)
        private set

    /** Mirror of the persisted greeting. */
    var greeting by mutableStateOf(preferences.greeting)
        private set

    /** True while the nuclear reset is running. */
    var isWiping by mutableStateOf(false)
        private set

    /** Set to true once a wipe completes; the screen consumes this to show a confirmation. */
    var wipeCompleted by mutableStateOf(false)
        private set

    fun updateDefaultCardioType(storageValue: String) {
        preferences.defaultCardioType = storageValue
        defaultCardioType = storageValue
    }

    fun updateLogName(value: String) {
        preferences.logName = value
        logName = value
    }

    fun updateGreeting(value: String) {
        preferences.greeting = value
        greeting = value
    }

    fun wipeAllData() {
        if (isWiping) return
        isWiping = true
        viewModelScope.launch {
            try {
                repository.wipeAllData()
                wipeCompleted = true
            } finally {
                isWiping = false
            }
        }
    }

    fun consumeWipeCompleted() {
        wipeCompleted = false
    }
}
