package com.example.nutritiontracker

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A food item the user has starred for quick access when logging frequently-eaten foods. */
@Entity(tableName = "favorite_foods")
data class FavoriteFood(
    @PrimaryKey val nummer: Int,
    val namn: String
)