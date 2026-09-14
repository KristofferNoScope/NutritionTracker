package com.example.nutritiontracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Insert
    suspend fun insert(entry: WeightEntry)

    @Delete
    suspend fun delete(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryForDate(date: String): WeightEntry?

    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    fun getAllEntries(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    fun getLatestEntry(): Flow<WeightEntry?>
}