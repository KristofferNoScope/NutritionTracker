package com.example.nutritiontracker

import retrofit2.Retrofit

object NetworkModule {

    val livsmedelsverketApi: LivsmedelsverketApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dataportal.livsmedelsverket.se/")
            .build()
            .create(LivsmedelsverketApi::class.java)
    }
}