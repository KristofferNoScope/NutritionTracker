package com.example.nutritiontracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    // --- Food item cache ---

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(items: List<FoodItem>)

    @Query("SELECT * FROM food_items WHERE namn LIKE '%' || :query || '%' LIMIT 30")
    suspend fun searchFoodItems(query: String): List<FoodItem>

    // --- Logged meals ---

    @Insert
    suspend fun insertLogEntry(entry: FoodLogEntry)

    @Delete
    suspend fun deleteLogEntry(entry: FoodLogEntry)

    @Query("SELECT * FROM food_log_entries WHERE date = :date ORDER BY id DESC")
    fun getLogEntriesForDate(date: String): Flow<List<FoodLogEntry>>
}