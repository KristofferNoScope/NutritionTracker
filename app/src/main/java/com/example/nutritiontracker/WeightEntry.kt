package com.example.nutritiontracker

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One weight measurement. Only one entry per day is kept (logging again the same day replaces it). */
@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val weightKg: Float
)