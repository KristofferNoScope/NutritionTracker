package com.example.nutritiontracker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "widget_settings")

enum class WidgetTheme {
    WHITE, BLACK, TRANSPARENT
}

object WidgetSettingsKeys {
    val THEME = stringPreferencesKey("widget_theme")
}

class WidgetSettingsRepository(private val context: Context) {

    val theme: Flow<WidgetTheme> = context.dataStore.data.map { prefs ->
        val saved = prefs[WidgetSettingsKeys.THEME]
        WidgetTheme.entries.find { it.name == saved } ?: WidgetTheme.WHITE
    }

    suspend fun saveTheme(theme: WidgetTheme) {
        context.dataStore.edit { prefs ->
            prefs[WidgetSettingsKeys.THEME] = theme.name
        }
    }
}