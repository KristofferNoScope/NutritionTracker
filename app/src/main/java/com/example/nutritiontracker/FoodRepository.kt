package com.example.nutritiontracker

import android.content.Context
import android.util.Log
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow

// EuroFIR codes for the nutrients we display in the app (see the API documentation).
private const val CODE_KCAL = "ENERC" // energy code; appears twice (kJ and kcal), see energyKcal()
private const val KJ_TO_KCAL = 0.239f
private const val CODE_PROTEIN = "PROT"
private const val CODE_FAT = "FAT"
private const val CODE_CARBS = "CHO"

// Number of items requested per page while paging through the full food list.
private const val PAGE_SIZE = 200

data class NutrientsPer100g(
    val kcal: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float
)

class FoodRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).foodDao()
    private val api = NetworkModule.livsmedelsverketApi

    /**
     * Fetches the full food list from the API (paging through all results, since the API
     * only returns a limited number of items per request) and caches it locally, but only
     * if the cache is empty (typically the first time the app is used).
     */
    suspend fun ensureFoodCacheLoaded() {
        val existingCount = dao.getFoodItemCount()
        Log.d("FoodLog", "Existing cache count: $existingCount")
        if (existingCount > 0) return

        val allItems = mutableListOf<FoodItem>()
        var offset = 0

        while (true) {
            val rawJson = api.getAllFoodsRaw(limit = PAGE_SIZE, offset = offset).string()
            val dataArray = JsonHelpers.findDataArray(rawJson)
            Log.d("FoodLog", "Page at offset $offset returned ${dataArray.length()} entries")

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                val nummer = obj.optInt("nummer", -1)
                val namn = obj.optString("namn", "")
                if (nummer != -1 && namn.isNotBlank()) {
                    allItems.add(FoodItem(nummer = nummer, namn = namn))
                }
            }

            if (dataArray.length() < PAGE_SIZE) break
            offset += PAGE_SIZE
        }

        Log.d("FoodLog", "Total parsed: ${allItems.size} food items")
        dao.insertFoodItems(allItems)
    }

    suspend fun searchFoods(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()
        val results = dao.searchFoodItems(query)
        Log.d("FoodLog", "Search for '$query' returned ${results.size} results")
        return results
    }

    /** Fetches nutrient values per 100g for a specific food item directly from the API. */
    suspend fun getNutrientsPer100g(nummer: Int): NutrientsPer100g {
        val rawJson = api.getNutrientsRaw(nummer).string()
        val array = JsonHelpers.findDataArray(rawJson)

        fun valueFor(code: String): Float {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("euroFIRkod") == code) {
                    return obj.optDouble("varde", 0.0).toFloat()
                }
            }
            return 0f
        }

        return NutrientsPer100g(
            kcal = energyKcal(array),
            protein = valueFor(CODE_PROTEIN),
            fat = valueFor(CODE_FAT),
            carbs = valueFor(CODE_CARBS)
        )
    }

    /**
     * The API returns energy as TWO rows sharing the same EuroFIR code (ENERC): one in kJ and
     * one in kcal. Taking the first match used to return kJ, which was then stored as kcal.
     * Here we identify the row by its unit instead, and fall back to converting from kJ
     * (kcal = kJ * 0.239, the factor Livsmedelsverket itself uses) if no kcal row exists.
     */
    private fun energyKcal(array: JSONArray): Float {
        var kcalValue: Float? = null
        var kjValue: Float? = null

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("euroFIRkod") != CODE_KCAL) continue

            // Look at every field that might describe the unit, since the exact field name
            // isn't documented. Lowercased so "kcal", "Kcal" and "Energi (kcal)" all match.
            val descriptor = listOf("enhet", "unit", "namn", "name")
                .joinToString(" ") { obj.optString(it, "") }
                .lowercase()
            val value = obj.optDouble("varde", 0.0).toFloat()

            Log.d("FoodLog", "ENERC row: $descriptor -> $value")

            when {
                "kcal" in descriptor -> if (kcalValue == null) kcalValue = value
                "kj" in descriptor -> if (kjValue == null) kjValue = value
            }
        }

        return kcalValue ?: kjValue?.let { it * KJ_TO_KCAL } ?: 0f
    }

    /** Logs a meal: converts nutrient values from per-100g to the actual amount and saves it. */
    suspend fun logFood(
        foodNummer: Int,
        foodName: String,
        grams: Float,
        date: String,
        per100g: NutrientsPer100g
    ) {
        val factor = grams / 100f
        dao.insertLogEntry(
            FoodLogEntry(
                foodNummer = foodNummer,
                foodName = foodName,
                grams = grams,
                date = date,
                kcal = per100g.kcal * factor,
                protein = per100g.protein * factor,
                fat = per100g.fat * factor,
                carbs = per100g.carbs * factor
            )
        )
    }

    /**
     * Updates an existing entry to a new gram amount, rescaling its already-stored nutrient
     * values proportionally rather than re-fetching from the API (faster, and avoids a
     * network dependency just to edit a number).
     */
    suspend fun updateLogEntryGrams(entry: FoodLogEntry, newGrams: Float) {
        if (entry.grams <= 0f || newGrams <= 0f) return
        val factor = newGrams / entry.grams
        dao.updateLogEntry(
            entry.copy(
                grams = newGrams,
                kcal = entry.kcal * factor,
                protein = entry.protein * factor,
                fat = entry.fat * factor,
                carbs = entry.carbs * factor
            )
        )
    }

    suspend fun deleteLogEntry(entry: FoodLogEntry) = dao.deleteLogEntry(entry)

    fun getLogEntriesForDate(date: String): Flow<List<FoodLogEntry>> =
        dao.getLogEntriesForDate(date)

    // --- Favorites ---

    suspend fun addFavorite(nummer: Int, namn: String) =
        dao.insertFavorite(FavoriteFood(nummer = nummer, namn = namn))

    suspend fun removeFavorite(nummer: Int) = dao.deleteFavoriteByNummer(nummer)

    fun getFavorites(): Flow<List<FavoriteFood>> = dao.getFavorites()

    fun isFavorite(nummer: Int): Flow<Boolean> = dao.isFavorite(nummer)
}