package com.example.nutritiontracker

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