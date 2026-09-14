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
}

class UserTargetsRepository(private val context: Context) {

    data class Settings(
        val mode: TargetMode,
        val manualKcal: Int,
        val manualProteinG: Int,
        val manualFatG: Int,
        val manualCarbsG: Int,
        val profile: UserProfile
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
            )
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
}