package com.example.nutritiontracker

// Step/distance goal is still fixed for now (not part of the nutrition targets feature).
const val DAILY_KM_TARGET = 5.0f

// Average distance covered per step, used to estimate km walked from the step count,
// since the device sensor only reports steps, not distance directly.
const val STRIDE_LENGTH_KM = 0.000762f