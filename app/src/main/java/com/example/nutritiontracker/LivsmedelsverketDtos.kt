package com.example.nutritiontracker

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** A single item from the list returned by GET /api/v1/livsmedel */
@JsonClass(generateAdapter = true)
data class FoodListItemDto(
    @Json(name = "Nummer") val nummer: Int,
    @Json(name = "Namn") val namn: String
)

/** A single nutrient from GET /api/v1/livsmedel/{nummer}/naringsvarden */
@JsonClass(generateAdapter = true)
data class NutrientDto(
    @Json(name = "Namn") val namn: String,
    @Json(name = "EuroFIRkod") val euroFirKod: String?,
    @Json(name = "Varde") val varde: Double?,
    @Json(name = "Enhet") val enhet: String?
)