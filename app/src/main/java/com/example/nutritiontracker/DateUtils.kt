package com.example.nutritiontracker

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Today's date as "yyyy-MM-dd", used to key food log entries per day. */
fun todayDateString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

/** Returns the date [days] away from [dateString] ("yyyy-MM-dd" in, "yyyy-MM-dd" out). */
fun shiftDate(dateString: String, days: Int): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val parsed = format.parse(dateString) ?: Date()
    val calendar = Calendar.getInstance()
    calendar.time = parsed
    calendar.add(Calendar.DAY_OF_MONTH, days)
    return format.format(calendar.time)
}

/** "Today" for today's date, otherwise a short readable date like "Mon, 14 Sep". */
fun formatDateForDisplay(dateString: String): String {
    if (dateString == todayDateString()) return "Today"
    val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    val parsed = inFormat.parse(dateString) ?: return dateString
    return outFormat.format(parsed)
}