package com.example.nutritiontracker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Today's date as "yyyy-MM-dd", used to key food log entries per day. */
fun todayDateString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())