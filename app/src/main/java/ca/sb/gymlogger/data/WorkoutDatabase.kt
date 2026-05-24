package ca.sb.gymlogger.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ---- Entities ----

@Entity(tableName = "lifting_sessions")
data class LiftingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    /** Calendar-day grouping key in YYYYMMDD form (e.g., 20260522). */
    val dateKey: Int = todayDateKey(),
    val exercises: String // JSON string
)

@Entity(tableName = "cardio_sessions")
data class CardioSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    /** Calendar-day grouping key in YYYYMMDD form. */
    val dateKey: Int = todayDateKey(),
    /** One of: "treadmill", "running", "cycling", "other". */
    val cardioType: String = "treadmill",
    /** Total session duration in minutes (free text, e.g. "45"). */
    val duration: String = "",
    /** Total session distance in miles (free text, e.g. "3.2"). */
    val distance: String = "",
    /** Total calories burned (free text, e.g. "380"). Optional. */
    val calories: String = "",
    /** JSON-encoded List<CardioPhase>. Empty/null for non-treadmill types. */
    val phases: String = "",
    val notes: String = ""
)

@Entity(tableName = "stretching_sessions")
data class StretchingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    /** Calendar-day grouping key in YYYYMMDD form. */
    val dateKey: Int = todayDateKey(),
    val stretches: String, // JSON string
    val notes: String
)

/**
 * A "quick-logged" workout day — created by long-pressing an empty day on the calendar.
 * Used when the user wants to flag that they worked out on a given day without
 * recording any details. Lives alongside the detailed session tables; the calendar
 * and history screens union all four.
 */
@Entity(tableName = "quick_logs")
data class QuickLog(
    @PrimaryKey val dateKey: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

/**
 * In-progress session state, persisted so a workout survives the app being backgrounded
 * or killed. One draft per session type; new sessions of the same type replace the
 * previous draft. Drafts are cleared when the user taps Finish (real session is saved).
 */
@Entity(tableName = "session_drafts")
data class SessionDraft(
    /** "weights", "cardio", or "stretching" — also the primary key. */
    @PrimaryKey val sessionType: String,
    val dateKey: Int,
    /** JSON-encoded type-specific payload. Shape depends on sessionType. */
    val payloadJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A reusable stretch name stored in the user's library.
 * Library entries are user-managed: every stretch they add through the picker is
 * saved here automatically, and they can edit or delete entries.
 */
@Entity(tableName = "stretch_library")
data class LibraryStretch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val displayOrder: Int = 0
)

/**
 * A reusable exercise name stored in the user's library.
 * Library entries are grouped by muscle group; the user can add, edit, delete any entry.
 * On first install the library is seeded with a starter set.
 */
@Entity(tableName = "exercise_library")
data class LibraryExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val muscleGroup: String,
    val displayOrder: Int = 0,
    /** True for entries inserted by first-launch seeding; false for user-added. Editing is allowed for both. */
    val isSeeded: Boolean = false
)

/**
 * A saved workout template — a reusable plan for a weights session.
 * Contains an ordered list of exercises with prescribed reps per set.
 */
@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    /** Used for sort order in the picker. Higher = first. Updated when user reorders. */
    val displayOrder: Int = 0,
    /** JSON-encoded List<TemplateExercise>. */
    val exercises: String = ""
)

// ---- DAOs ----

@Dao
interface LiftingDao {
    @Insert
    suspend fun insert(session: LiftingSession)

    @Update
    suspend fun update(session: LiftingSession)

    @Delete
    suspend fun delete(session: LiftingSession)

    @Query("SELECT * FROM lifting_sessions WHERE id = :id")
    suspend fun getById(id: Int): LiftingSession?

    @Query("SELECT * FROM lifting_sessions ORDER BY date DESC")
    fun getAll(): Flow<List<LiftingSession>>
}

@Dao
interface CardioDao {
    @Insert
    suspend fun insert(session: CardioSession)

    @Update
    suspend fun update(session: CardioSession)

    @Delete
    suspend fun delete(session: CardioSession)

    @Query("SELECT * FROM cardio_sessions WHERE id = :id")
    suspend fun getById(id: Int): CardioSession?

    @Query("SELECT * FROM cardio_sessions ORDER BY date DESC")
    fun getAll(): Flow<List<CardioSession>>
}

@Dao
interface StretchingDao {
    @Insert
    suspend fun insert(session: StretchingSession)

    @Update
    suspend fun update(session: StretchingSession)

    @Delete
    suspend fun delete(session: StretchingSession)

    @Query("SELECT * FROM stretching_sessions WHERE id = :id")
    suspend fun getById(id: Int): StretchingSession?

    @Query("SELECT * FROM stretching_sessions ORDER BY date DESC")
    fun getAll(): Flow<List<StretchingSession>>
}

@Dao
interface QuickLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quickLog: QuickLog)

    @Delete
    suspend fun delete(quickLog: QuickLog)

    @Query("DELETE FROM quick_logs WHERE dateKey = :dateKey")
    suspend fun deleteByDateKey(dateKey: Int)

    @Query("SELECT * FROM quick_logs ORDER BY dateKey DESC")
    fun getAll(): Flow<List<QuickLog>>

    @Query("SELECT * FROM quick_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDateKey(dateKey: Int): QuickLog?
}

@Dao
interface SessionDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: SessionDraft)

    @Query("SELECT * FROM session_drafts WHERE sessionType = :type LIMIT 1")
    suspend fun get(type: String): SessionDraft?

    @Query("DELETE FROM session_drafts WHERE sessionType = :type")
    suspend fun delete(type: String)
}

@Dao
interface LibraryStretchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stretch: LibraryStretch): Long

    @Update
    suspend fun update(stretch: LibraryStretch)

    @Delete
    suspend fun delete(stretch: LibraryStretch)

    @Query("SELECT * FROM stretch_library ORDER BY displayOrder DESC, name ASC")
    fun getAll(): Flow<List<LibraryStretch>>

    @Query("SELECT * FROM stretch_library WHERE id = :id")
    suspend fun getById(id: Int): LibraryStretch?

    @Query("SELECT * FROM stretch_library WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): LibraryStretch?
}

@Dao
interface LibraryExerciseDao {
    @Insert
    suspend fun insert(exercise: LibraryExercise): Long

    @Insert
    suspend fun insertAll(exercises: List<LibraryExercise>)

    @Update
    suspend fun update(exercise: LibraryExercise)

    @Delete
    suspend fun delete(exercise: LibraryExercise)

    @Query("SELECT * FROM exercise_library ORDER BY muscleGroup ASC, displayOrder DESC, name ASC")
    fun getAll(): Flow<List<LibraryExercise>>

    @Query("SELECT * FROM exercise_library WHERE id = :id")
    suspend fun getById(id: Int): LibraryExercise?

    @Query("SELECT COUNT(*) FROM exercise_library")
    suspend fun count(): Int
}

@Dao
interface WorkoutTemplateDao {
    @Insert
    suspend fun insert(template: WorkoutTemplate): Long

    @Update
    suspend fun update(template: WorkoutTemplate)

    @Delete
    suspend fun delete(template: WorkoutTemplate)

    @Query("SELECT * FROM workout_templates ORDER BY displayOrder DESC, id ASC")
    fun getAll(): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getById(id: Int): WorkoutTemplate?
}

// ---- Database ----

@Database(
    entities = [
        LiftingSession::class,
        CardioSession::class,
        StretchingSession::class,
        WorkoutTemplate::class,
        LibraryExercise::class,
        LibraryStretch::class,
        SessionDraft::class,
        QuickLog::class
    ],
    version = 8,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun liftingDao(): LiftingDao
    abstract fun cardioDao(): CardioDao
    abstract fun stretchingDao(): StretchingDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun libraryExerciseDao(): LibraryExerciseDao
    abstract fun libraryStretchDao(): LibraryStretchDao
    abstract fun sessionDraftDao(): SessionDraftDao
    abstract fun quickLogDao(): QuickLogDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                    // Wipe and recreate on schema change rather than writing migrations.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * The default exercise library, drawn from the user's spreadsheet plus common additions.
 * Used by [WorkoutRepository.seedLibraryIfEmpty] on first use.
 */
internal fun seedLibraryRows(): List<Pair<String, String>> = listOf(
    // Chest
    "DB Bench Press" to "Chest",
    "DB Incline Bench Press" to "Chest",
    "BB Bench Press" to "Chest",
    "DB Flys" to "Chest",
    "Cable Crossovers" to "Chest",
    "Chest Dip (Front Pushdown)" to "Chest",
    // Back
    "Cable Row (Low)" to "Back",
    "Cable Row (Neck)" to "Back",
    "Lat Pulldown" to "Back",
    "Upright Row" to "Back",
    "Machine Reverse Fly" to "Back",
    "Back Extension" to "Back",
    "Ukrainian Deadlifts" to "Back",
    "One-Arm DB Row" to "Back",
    // Shoulders
    "BB Front Raise" to "Shoulders",
    "Side Raises (Machine)" to "Shoulders",
    "DB Press (Seated)" to "Shoulders",
    "DB Lateral Raises" to "Shoulders",
    // Biceps
    "BB Curl (Standing)" to "Biceps",
    "Preacher Curl" to "Biceps",
    "Incline DB Curl" to "Biceps",
    // Triceps
    "Seated Tricep Press" to "Triceps",
    "Cable Overhead Tricep Ext." to "Triceps",
    "Tricep Cable Pushdowns" to "Triceps",
    "Tricep Dips" to "Triceps",
    "Skullcrushers (Bench)" to "Triceps",
    "DB Extensions" to "Triceps",
    // Legs
    "Squat" to "Legs",
    "DB Lunge" to "Legs",
    "Leg Press" to "Legs",
    "Leg Curl" to "Legs",
    "Leg Extension" to "Legs",
    // Calves
    "Seated Calf Raise" to "Calves",
    "Standing Calf Raise" to "Calves",
    // Abs
    "Ab Machine" to "Abs"
)