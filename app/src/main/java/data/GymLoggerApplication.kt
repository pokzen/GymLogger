package ca.bpmproperty.gymlogger.data

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymLoggerApplication : Application() {
    val database by lazy { WorkoutDatabase.getDatabase(this) }
    val repository by lazy { WorkoutRepository(database) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Idempotent: seeds the exercise library on first launch (or after a destructive
        // migration wipes it). No-op if library already has entries.
        applicationScope.launch {
            repository.seedLibraryIfEmpty()
        }
    }
}