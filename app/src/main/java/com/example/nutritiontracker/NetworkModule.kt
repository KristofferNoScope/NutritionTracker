package com.example.nutritiontracker

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val livsmedelsverketApi: LivsmedelsverketApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dataportal.livsmedelsverket.se/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LivsmedelsverketApi::class.java)
    }
}