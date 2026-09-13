package com.example.nutritiontracker

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LivsmedelsverketApi {

    // Returned as raw JSON (ResponseBody) instead of a typed list, because the API wraps
    // the data array in an envelope object alongside pagination links. See
    // JsonHelpers.findDataArray for how we handle that generically.
    @GET("livsmedel/api/v1/livsmedel")
    suspend fun getAllFoodsRaw(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): ResponseBody

    @GET("livsmedel/api/v1/livsmedel/{nummer}/naringsvarden")
    suspend fun getNutrientsRaw(@Path("nummer") nummer: Int): ResponseBody
}