package com.example.nutritiontracker

import androidx.datastore.preferences.core.stringPreferencesKey

enum class WidgetTheme {
    WHITE, BLACK, TRANSPARENT
}

object WidgetSettingsKeys {
    val THEME_KEY = stringPreferencesKey("widget_theme")
}