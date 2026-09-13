package com.example.nutritiontracker

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of food items from Livsmedelsverket (id + name)
 * Get once från API and saved here. FoodLogScreen will quick and not need internet connection.
 */
@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey val nummer: Int,
    val namn: String
)