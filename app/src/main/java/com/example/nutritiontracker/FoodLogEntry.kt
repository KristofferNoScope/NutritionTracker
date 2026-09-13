package com.example.nutritiontracker

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single row in the food log. Nutrient values (kcal/protein/fat/carbs) are stored
 * already calculated for the actual amount eaten (grams) - not per 100g - so that
 * history doesn't change later if the Swedish Food Agency updates their database.
 */
@Entity(tableName = "food_log_entries")
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodNummer: Int,
    val foodName: String,
    val grams: Float,
    val date: String, // format "yyyy-MM-dd", used to filter entries per day
    val kcal: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float
)