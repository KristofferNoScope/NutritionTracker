package com.example.nutritiontracker

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface LivsmedelsverketApi {

    // Returned as raw JSON (ResponseBody) instead of a typed list, because the API wraps
    // the array in an envelope object whose exact field name isn't documented. See
    // JsonHelpers.findFirstJsonArray for how we handle that generically.
    @GET("livsmedel/api/v1/livsmedel")
    suspend fun getAllFoodsRaw(): ResponseBody

    @GET("livsmedel/api/v1/livsmedel/{nummer}/naringsvarden")
    suspend fun getNutrientsRaw(@Path("nummer") nummer: Int): ResponseBody
}