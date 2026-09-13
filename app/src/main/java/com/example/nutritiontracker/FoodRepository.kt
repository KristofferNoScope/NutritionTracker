package com.example.nutritiontracker

import android.content.Context
import kotlinx.coroutines.flow.Flow

// EuroFIR codes for the nutrients we display in the app (see the API documentation).
private const val CODE_KCAL = "ENERC"
private const val CODE_PROTEIN = "PROT"
private const val CODE_FAT = "FAT"
private const val CODE_CARBS = "CHO"

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
     * Fetches the full food list from the API and caches it locally,
     * but only if the cache is empty (typically the first time the app is used).
     */
    suspend fun ensureFoodCacheLoaded() {
        if (dao.getFoodItemCount() == 0) {
            val rawJson = api.getAllFoodsRaw().string()
            val array = JsonHelpers.findFirstJsonArray(rawJson)

            val items = mutableListOf<FoodItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val nummer = obj.optInt("Nummer", -1)
                val namn = obj.optString("Namn", "")
                if (nummer != -1 && namn.isNotBlank()) {
                    items.add(FoodItem(nummer = nummer, namn = namn))
                }
            }
            dao.insertFoodItems(items)
        }
    }

    suspend fun searchFoods(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()
        return dao.searchFoodItems(query)
    }

    /** Fetches nutrient values per 100g for a specific food item directly from the API. */
    suspend fun getNutrientsPer100g(nummer: Int): NutrientsPer100g {
        val rawJson = api.getNutrientsRaw(nummer).string()
        val array = JsonHelpers.findFirstJsonArray(rawJson)

        fun valueFor(code: String): Float {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("EuroFIRkod") == code) {
                    return obj.optDouble("Varde", 0.0).toFloat()
                }
            }
            return 0f
        }

        return NutrientsPer100g(
            kcal = valueFor(CODE_KCAL),
            protein = valueFor(CODE_PROTEIN),
            fat = valueFor(CODE_FAT),
            carbs = valueFor(CODE_CARBS)
        )
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

    suspend fun deleteLogEntry(entry: FoodLogEntry) = dao.deleteLogEntry(entry)

    fun getLogEntriesForDate(date: String): Flow<List<FoodLogEntry>> =
        dao.getLogEntriesForDate(date)
}