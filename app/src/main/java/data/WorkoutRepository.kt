package ca.bpmproperty.gymlogger.data

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