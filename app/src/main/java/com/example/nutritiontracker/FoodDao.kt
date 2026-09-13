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

    // Ranks results so names starting with the query (e.g. "Kycklingfilé" for "kyckling")
    // come before names that merely contain it somewhere in a longer compound dish name.
    // Shorter names are tie-broken first, since they're usually the more literal/plain match.
    @Query(
        """
        SELECT * FROM food_items
        WHERE namn LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN namn LIKE :query || '%' THEN 0 ELSE 1 END,
            LENGTH(namn) ASC
        LIMIT 30
        """
    )
    suspend fun searchFoodItems(query: String): List<FoodItem>

    // --- Favorites ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteFood)

    @Query("DELETE FROM favorite_foods WHERE nummer = :nummer")
    suspend fun deleteFavoriteByNummer(nummer: Int)

    @Query("SELECT * FROM favorite_foods ORDER BY namn ASC")
    fun getFavorites(): Flow<List<FavoriteFood>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_foods WHERE nummer = :nummer)")
    fun isFavorite(nummer: Int): Flow<Boolean>

    // --- Logged meals ---

    @Insert
    suspend fun insertLogEntry(entry: FoodLogEntry)

    @Delete
    suspend fun deleteLogEntry(entry: FoodLogEntry)

    @Query("SELECT * FROM food_log_entries WHERE date = :date ORDER BY id DESC")
    fun getLogEntriesForDate(date: String): Flow<List<FoodLogEntry>>
}