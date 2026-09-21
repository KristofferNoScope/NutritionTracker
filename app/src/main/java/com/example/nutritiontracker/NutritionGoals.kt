package com.example.nutritiontracker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

enum class TargetMode { MANUAL, CALCULATED }

enum class Sex(val label: String) {
    MALE("Male"), FEMALE("Female")
}

enum class ActivityLevel(val multiplier: Float, val label: String) {
    SEDENTARY(1.2f, "Sedentary (little/no exercise)"),
    LIGHT(1.375f, "Light (1-3 days/week)"),
    MODERATE(1.55f, "Moderate (3-5 days/week)"),
    VERY_ACTIVE(1.725f, "Very active (6-7 days/week)"),
    EXTRA_ACTIVE(1.9f, "Extra active (hard training + physical job)")
}

enum class Goal(val label: String) {
    BULK("Bulk"), CUT("Cut"), MAINTAIN("Maintain")
}

data class UserProfile(
    val weightKg: Float,
    val heightCm: Float,
    val age: Int,
    val sex: Sex,
    val activityLevel: ActivityLevel,
    val goal: Goal
)

data class NutrientTargets(
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int
)

/**
 * Calculates daily nutrition targets from a user profile using established formulas:
 * - BMR via the Mifflin-St Jeor equation
 * - TDEE = BMR x activity multiplier
 * - Calorie target = TDEE adjusted for the goal (surplus for bulk, deficit for cut)
 * - Protein target is set higher than general guidelines (2g/kg bodyweight), common
 *   practice for resistance training / bodybuilding
 * - Fat is set to 25% of total calories
 * - Carbs fill the remaining calories
 */
object TargetsCalculator {

    fun calculate(profile: UserProfile): NutrientTargets {
        val bmr = when (profile.sex) {
            Sex.MALE -> 10 * profile.weightKg + 6.25f * profile.heightCm - 5 * profile.age + 5
            Sex.FEMALE -> 10 * profile.weightKg + 6.25f * profile.heightCm - 5 * profile.age - 161
        }

        val tdee = bmr * profile.activityLevel.multiplier

        val kcalTarget = when (profile.goal) {
            Goal.BULK -> tdee + 350
            Goal.CUT -> tdee - 500
            Goal.MAINTAIN -> tdee
        }

        val proteinG = profile.weightKg * 2.0f
        val fatG = (kcalTarget * 0.25f) / 9f

        val proteinKcal = proteinG * 4f
        val fatKcal = fatG * 9f
        val carbsG = ((kcalTarget - proteinKcal - fatKcal) / 4f).coerceAtLeast(0f)

        return NutrientTargets(
            kcal = kcalTarget.roundToInt(),
            proteinG = proteinG.roundToInt(),
            fatG = fatG.roundToInt(),
            carbsG = carbsG.roundToInt()
        )
    }
}

private val Context.userTargetsDataStore by preferencesDataStore(name = "user_targets")

private object TargetKeys {
    val MODE = stringPreferencesKey("mode")
    val MANUAL_KCAL = intPreferencesKey("manual_kcal")
    val MANUAL_PROTEIN = intPreferencesKey("manual_protein")
    val MANUAL_FAT = intPreferencesKey("manual_fat")
    val MANUAL_CARBS = intPreferencesKey("manual_carbs")
    val WEIGHT_KG = floatPreferencesKey("weight_kg")
    val HEIGHT_CM = floatPreferencesKey("height_cm")
    val AGE = intPreferencesKey("age")
    val SEX = stringPreferencesKey("sex")
    val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
    val GOAL = stringPreferencesKey("goal")
    val STEP_GOAL_KM = floatPreferencesKey("step_goal_km")
}

class UserTargetsRepository(private val context: Context) {

    data class Settings(
        val mode: TargetMode,
        val manualKcal: Int,
        val manualProteinG: Int,
        val manualFatG: Int,
        val manualCarbsG: Int,
        val profile: UserProfile,
        val stepGoalKm: Float
    )

    // Defaults match the app's original hardcoded values, so behavior is unchanged
    // for anyone who hasn't opened the settings screen yet.
    val settings: Flow<Settings> = context.userTargetsDataStore.data.map { prefs ->
        Settings(
            mode = TargetMode.entries.find { it.name == prefs[TargetKeys.MODE] } ?: TargetMode.MANUAL,
            manualKcal = prefs[TargetKeys.MANUAL_KCAL] ?: 2000,
            manualProteinG = prefs[TargetKeys.MANUAL_PROTEIN] ?: 120,
            manualFatG = prefs[TargetKeys.MANUAL_FAT] ?: 70,
            manualCarbsG = prefs[TargetKeys.MANUAL_CARBS] ?: 250,
            profile = UserProfile(
                weightKg = prefs[TargetKeys.WEIGHT_KG] ?: 75f,
                heightCm = prefs[TargetKeys.HEIGHT_CM] ?: 175f,
                age = prefs[TargetKeys.AGE] ?: 25,
                sex = Sex.entries.find { it.name == prefs[TargetKeys.SEX] } ?: Sex.MALE,
                activityLevel = ActivityLevel.entries.find {
                    it.name == prefs[TargetKeys.ACTIVITY_LEVEL]
                } ?: ActivityLevel.MODERATE,
                goal = Goal.entries.find { it.name == prefs[TargetKeys.GOAL] } ?: Goal.MAINTAIN
            ),
            stepGoalKm = prefs[TargetKeys.STEP_GOAL_KM] ?: DAILY_KM_TARGET
        )
    }

    val effectiveTargets: Flow<NutrientTargets> = settings.map { s ->
        when (s.mode) {
            TargetMode.MANUAL -> NutrientTargets(s.manualKcal, s.manualProteinG, s.manualFatG, s.manualCarbsG)
            TargetMode.CALCULATED -> TargetsCalculator.calculate(s.profile)
        }
    }

    /** One-shot read, for use in short-lived contexts like the widget. */
    suspend fun getEffectiveTargetsOnce(): NutrientTargets = effectiveTargets.first()

    /** Daily distance goal in km. Independent of the nutrition target mode. */
    val stepGoalKm: Flow<Float> = settings.map { it.stepGoalKm }

    suspend fun getStepGoalKmOnce(): Float = stepGoalKm.first()

    suspend fun save(
        mode: TargetMode,
        manualKcal: Int,
        manualProteinG: Int,
        manualFatG: Int,
        manualCarbsG: Int,
        profile: UserProfile
    ) {
        context.userTargetsDataStore.edit { prefs ->
            prefs[TargetKeys.MODE] = mode.name
            prefs[TargetKeys.MANUAL_KCAL] = manualKcal
            prefs[TargetKeys.MANUAL_PROTEIN] = manualProteinG
            prefs[TargetKeys.MANUAL_FAT] = manualFatG
            prefs[TargetKeys.MANUAL_CARBS] = manualCarbsG
            prefs[TargetKeys.WEIGHT_KG] = profile.weightKg
            prefs[TargetKeys.HEIGHT_CM] = profile.heightCm
            prefs[TargetKeys.AGE] = profile.age
            prefs[TargetKeys.SEX] = profile.sex.name
            prefs[TargetKeys.ACTIVITY_LEVEL] = profile.activityLevel.name
            prefs[TargetKeys.GOAL] = profile.goal.name
        }
    }

    suspend fun saveStepGoalKm(km: Float) {
        context.userTargetsDataStore.edit { prefs ->
            prefs[TargetKeys.STEP_GOAL_KM] = km
        }
    }
}