package com.example.nutritiontracker

import android.content.Context
import kotlinx.coroutines.flow.Flow

class WeightRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).weightDao()

    /** Logs weight for the given date. If an entry for that date already exists, it's replaced. */
    suspend fun logWeightForDate(date: String, weightKg: Float) {
        val existing = dao.getEntryForDate(date)
        if (existing != null) {
            dao.delete(existing)
        }
        dao.insert(WeightEntry(date = date, weightKg = weightKg))
    }

    suspend fun deleteEntry(entry: WeightEntry) = dao.delete(entry)

    fun getAllEntries(): Flow<List<WeightEntry>> = dao.getAllEntries()

    fun getLatestEntry(): Flow<WeightEntry?> = dao.getLatestEntry()
}