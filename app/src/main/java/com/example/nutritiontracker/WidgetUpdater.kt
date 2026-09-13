package com.example.nutritiontracker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

/** Forces every instance of the nutrition widget to re-render with fresh data right away. */
suspend fun updateNutritionWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context.applicationContext)
    val glanceIds = manager.getGlanceIds(NutritionWidget::class.java)
    glanceIds.forEach { glanceId ->
        NutritionWidget().update(context.applicationContext, glanceId)
    }
}