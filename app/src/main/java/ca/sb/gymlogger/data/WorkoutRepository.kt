package ca.sb.gymlogger.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val db: WorkoutDatabase) {

    // Lifting
    suspend fun insertLifting(session: LiftingSession) = db.liftingDao().insert(session)
    suspend fun updateLifting(session: LiftingSession) = db.liftingDao().update(session)
    suspend fun deleteLifting(session: LiftingSession) = db.liftingDao().delete(session)
    suspend fun getLifting(id: Int): LiftingSession? = db.liftingDao().getById(id)
    fun getAllLifting(): Flow<List<LiftingSession>> = db.liftingDao().getAll()

    // Cardio
    suspend fun insertCardio(session: CardioSession) = db.cardioDao().insert(session)
    suspend fun updateCardio(session: CardioSession) = db.cardioDao().update(session)
    suspend fun deleteCardio(session: CardioSession) = db.cardioDao().delete(session)
    suspend fun getCardio(id: Int): CardioSession? = db.cardioDao().getById(id)
    fun getAllCardio(): Flow<List<CardioSession>> = db.cardioDao().getAll()

    // Stretching
    suspend fun insertStretching(session: StretchingSession) = db.stretchingDao().insert(session)
    suspend fun updateStretching(session: StretchingSession) = db.stretchingDao().update(session)
    suspend fun deleteStretching(session: StretchingSession) = db.stretchingDao().delete(session)
    suspend fun getStretching(id: Int): StretchingSession? = db.stretchingDao().getById(id)
    fun getAllStretching(): Flow<List<StretchingSession>> = db.stretchingDao().getAll()

    // Workout Templates
    fun getAllTemplates(): Flow<List<WorkoutTemplate>> = db.workoutTemplateDao().getAll()
    suspend fun getTemplate(id: Int): WorkoutTemplate? = db.workoutTemplateDao().getById(id)
    suspend fun insertTemplate(template: WorkoutTemplate): Long = db.workoutTemplateDao().insert(template)
    suspend fun updateTemplate(template: WorkoutTemplate) = db.workoutTemplateDao().update(template)
    suspend fun deleteTemplate(template: WorkoutTemplate) = db.workoutTemplateDao().delete(template)

    // Exercise Library
    fun getAllLibraryExercises(): Flow<List<LibraryExercise>> = db.libraryExerciseDao().getAll()
    suspend fun getLibraryExercise(id: Int): LibraryExercise? = db.libraryExerciseDao().getById(id)
    suspend fun insertLibraryExercise(exercise: LibraryExercise): Long = db.libraryExerciseDao().insert(exercise)
    suspend fun updateLibraryExercise(exercise: LibraryExercise) = db.libraryExerciseDao().update(exercise)
    suspend fun deleteLibraryExercise(exercise: LibraryExercise) = db.libraryExerciseDao().delete(exercise)

    // Stretch Library
    fun getAllLibraryStretches(): Flow<List<LibraryStretch>> = db.libraryStretchDao().getAll()
    suspend fun getLibraryStretch(id: Int): LibraryStretch? = db.libraryStretchDao().getById(id)
    suspend fun updateLibraryStretch(stretch: LibraryStretch) = db.libraryStretchDao().update(stretch)
    suspend fun deleteLibraryStretch(stretch: LibraryStretch) = db.libraryStretchDao().delete(stretch)

    // Session Drafts (in-progress session persistence)
    suspend fun getDraft(type: String): SessionDraft? = db.sessionDraftDao().get(type)
    suspend fun upsertDraft(draft: SessionDraft) = db.sessionDraftDao().upsert(draft)
    suspend fun deleteDraft(type: String) = db.sessionDraftDao().delete(type)

    // Quick Logs — "I worked out on this day, no details"
    fun getAllQuickLogs(): Flow<List<QuickLog>> = db.quickLogDao().getAll()
    suspend fun getQuickLog(dateKey: Int): QuickLog? = db.quickLogDao().getByDateKey(dateKey)
    suspend fun upsertQuickLog(quickLog: QuickLog) = db.quickLogDao().upsert(quickLog)
    suspend fun deleteQuickLog(dateKey: Int) = db.quickLogDao().deleteByDateKey(dateKey)

    /**
     * Wipes all session-style data: lifting, cardio, stretching, and quick-logs.
     * Templates, library entries, and in-progress drafts are NOT touched — they're
     * not part of the export/import payload and persist across imports.
     */
    suspend fun wipeAllSessions() {
        db.liftingDao().deleteAll()
        db.cardioDao().deleteAll()
        db.stretchingDao().deleteAll()
        db.quickLogDao().deleteAll()
    }

    /**
     * Nuclear reset. Wipes everything in the database — sessions, quick-logs,
     * templates, library entries, in-progress drafts — then re-seeds the default
     * exercise library so the app isn't completely empty. Used by Settings → Erase
     * all data.
     */
    suspend fun wipeAllData() {
        db.liftingDao().deleteAll()
        db.cardioDao().deleteAll()
        db.stretchingDao().deleteAll()
        db.quickLogDao().deleteAll()
        db.workoutTemplateDao().deleteAll()
        db.libraryStretchDao().deleteAll()
        db.libraryExerciseDao().deleteAll()
        db.sessionDraftDao().deleteAll()
        // Re-seed the default library so first lifting log isn't a blank slate.
        seedLibraryIfEmpty()
    }

    /**
     * Insert a stretch into the library if a matching name doesn't already exist.
     * Case-insensitive match. Returns true if inserted, false if a duplicate was found.
     */
    suspend fun saveLibraryStretchIfNew(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val existing = db.libraryStretchDao().findByName(trimmed)
        if (existing != null) return false
        db.libraryStretchDao().insert(LibraryStretch(name = trimmed))
        return true
    }

    /** Idempotent. Populates the library with default exercises if it's currently empty. */
    suspend fun seedLibraryIfEmpty() {
        val dao = db.libraryExerciseDao()
        if (dao.count() > 0) return
        val rows = seedLibraryRows().map { (name, group) ->
            LibraryExercise(name = name, muscleGroup = group, isSeeded = true)
        }
        dao.insertAll(rows)
    }
}