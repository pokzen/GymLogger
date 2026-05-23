package ca.bpmproperty.gymlogger.data

import kotlinx.serialization.Serializable

// ---- Lifting ----

@Serializable
data class SetEntry(val reps: String, val weight: String)

@Serializable
data class ExerciseEntry(
    val name: String,
    val sets: List<SetEntry>,
    /** Optional muscle group for in-session display grouping. Defaulted so old sessions deserialize cleanly. */
    val muscleGroup: String = ""
)

// ---- Stretching ----

@Serializable
data class StretchEntry(val name: String, val duration: String)

// ---- Workout Templates ----

/**
 * One exercise in a saved workout template.
 * `prescribedReps` is an ordered list — set 1 prescribes prescribedReps[0],
 * set 2 prescribes prescribedReps[1], etc.
 */
@Serializable
data class TemplateExercise(
    val name: String,
    val muscleGroup: String = "",
    val prescribedReps: List<Int> = emptyList()
)

// ---- Cardio (treadmill phases) ----

/**
 * One phase of a treadmill session. Speed and incline are stored as start/end
 * pairs so we can capture both steady phases (start == end) and progressions
 * (start != end) without the user having to think about which kind it was.
 *
 * All fields are free-text strings so the user can type "3.5" or leave blank.
 */
@Serializable
data class CardioPhase(
    val label: String = "",
    val durationMin: String = "",
    val speedStart: String = "",
    val speedEnd: String = "",
    val inclineStart: String = "",
    val inclineEnd: String = ""
)
