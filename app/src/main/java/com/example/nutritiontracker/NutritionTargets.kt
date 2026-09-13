package com.example.nutritiontracker

// Daily nutrient goals. Hardcoded for now - could later become user-configurable.
const val DAILY_KCAL_TARGET = 2000
const val DAILY_PROTEIN_TARGET_G = 120
const val DAILY_CARBS_TARGET_G = 250
const val DAILY_FAT_TARGET_G = 70
const val DAILY_KM_TARGET = 5.0f

// Average distance covered per step, used to estimate km walked from the step count,
// since the device sensor only reports steps, not distance directly.
const val STRIDE_LENGTH_KM = 0.000762f