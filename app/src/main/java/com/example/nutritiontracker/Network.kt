package com.example.nutritiontracker

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

object NetworkModule {

    val livsmedelsverketApi: LivsmedelsverketApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dataportal.livsmedelsverket.se/")
            .build()
            .create(LivsmedelsverketApi::class.java)
    }

    // Open Food Facts asks every client to identify itself with a custom User-Agent.
    val openFoodFactsApi: OpenFoodFactsApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "NutritionTracker/1.0 (Android)")
                        .build()
                )
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client)
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}

interface OpenFoodFactsApi {

    // Raw JSON again, parsed by hand in FoodRepository (same approach as the Livsmedelsverket API).
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductRaw(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String
    ): ResponseBody
}

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

/**
 * Some public APIs (including the Swedish Food Agency's) wrap a JSON array in an envelope
 * object (e.g. {"livsmedel": [...], "_links": [...]}) without documenting the exact field
 * name. Instead of hardcoding a guessed field name, we find the array of actual data by
 * skipping any array that looks like HATEOAS pagination links (objects with an "href" key),
 * and picking the largest remaining candidate.
 */
object JsonHelpers {

    fun findDataArray(rawJson: String): JSONArray {
        return when (val root = JSONTokener(rawJson).nextValue()) {
            is JSONArray -> root
            is JSONObject -> {
                val candidates = root.keys().asSequence()
                    .mapNotNull { key -> (root.opt(key) as? JSONArray)?.let { key to it } }
                    .toList()

                if (candidates.isEmpty()) {
                    throw IllegalStateException(
                        "No array field found in JSON response. Top-level keys: ${root.keys().asSequence().toList()}"
                    )
                }

                val nonLinkCandidates = candidates.filterNot { (_, array) ->
                    array.length() > 0 && array.optJSONObject(0)?.has("href") == true
                }

                (nonLinkCandidates.ifEmpty { candidates })
                    .maxByOrNull { it.second.length() }!!
                    .second
            }
            else -> throw IllegalStateException("Unexpected JSON root type: ${root::class.simpleName}")
        }
    }
}