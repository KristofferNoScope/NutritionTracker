package com.example.nutritiontracker

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager

enum class WidgetTheme {
    WHITE, BLACK, TRANSPARENT
}

object WidgetSettingsKeys {
    val THEME_KEY = stringPreferencesKey("widget_theme")
}

/** Forces every instance of the nutrition widget to re-render with fresh data right away. */
suspend fun updateNutritionWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context.applicationContext)
    val glanceIds = manager.getGlanceIds(NutritionWidget::class.java)
    glanceIds.forEach { glanceId ->
        NutritionWidget().update(context.applicationContext, glanceId)
    }

    // Send a short delayed follow-up update too. If several updates fire in quick
    // succession (e.g. deleting multiple entries back to back), Android's widget host
    // can occasionally coalesce/drop an intermediate one - this catches that case.
    kotlinx.coroutines.delay(500)
    glanceIds.forEach { glanceId ->
        NutritionWidget().update(context.applicationContext, glanceId)
    }
}