package com.example.nutritiontracker

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

class NutritionWidget : GlanceAppWidget() {

    // Glance handles storing and syncing the theme internally - no custom DataStore needed.
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fetch real data before composing - these are one-shot reads (not live streams),
        // since the widget only re-renders when explicitly updated or on the periodic
        // system refresh, not continuously like the app screen.
        val foodRepository = FoodRepository(context)
        val stepsRepository = StepsRepository(context)

        val todaysEntries = foodRepository.getLogEntriesForDate(todayDateString()).first()
        val totalKcal = todaysEntries.sumOf { it.kcal.toDouble() }.roundToInt()
        val totalProtein = todaysEntries.sumOf { it.protein.toDouble() }.roundToInt()
        val totalFat = todaysEntries.sumOf { it.fat.toDouble() }.roundToInt()
        val totalCarbs = todaysEntries.sumOf { it.carbs.toDouble() }.roundToInt()

        val todaySteps = stepsRepository.getTodaySteps()
        val currentKm = todaySteps * STRIDE_LENGTH_KM

        provideContent {
            val prefs = currentState<Preferences>()
            val theme = WidgetTheme.entries.find {
                it.name == prefs[WidgetSettingsKeys.THEME_KEY]
            } ?: WidgetTheme.WHITE

            val backgroundColor = when (theme) {
                WidgetTheme.WHITE -> Color.White
                WidgetTheme.BLACK -> Color.Black
                WidgetTheme.TRANSPARENT -> Color.Transparent
            }
            val textColor = when (theme) {
                WidgetTheme.WHITE -> Color.Black
                WidgetTheme.BLACK, WidgetTheme.TRANSPARENT -> Color.White
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(day = backgroundColor, night = backgroundColor))
                    .padding(12.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            ) {
                Text(
                    text = "Today's Steps",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = textColor, night = textColor)
                    )
                )

                ProgressBar(label = "Km", current = currentKm, target = DAILY_KM_TARGET, color = Color(0xFF9C27B0), textColor = textColor, isDecimal = true, topPadding = 4.dp)
                ProgressBar(label = "Kcal", current = totalKcal.toFloat(), target = DAILY_KCAL_TARGET.toFloat(), color = Color(0xFF4CAF50), textColor = textColor, topPadding = 10.dp)
                ProgressBar(label = "Protein", current = totalProtein.toFloat(), target = DAILY_PROTEIN_TARGET_G.toFloat(), color = Color(0xFF2196F3), textColor = textColor, topPadding = 6.dp)
                ProgressBar(label = "Carbs", current = totalCarbs.toFloat(), target = DAILY_CARBS_TARGET_G.toFloat(), color = Color(0xFFFFC107), textColor = textColor, topPadding = 6.dp)
                ProgressBar(label = "Fat", current = totalFat.toFloat(), target = DAILY_FAT_TARGET_G.toFloat(), color = Color(0xFFF44336), textColor = textColor, topPadding = 6.dp)
            }
        }
    }
}

@Composable
private fun ProgressBar(
    label: String,
    current: Float,
    target: Float,
    color: Color,
    textColor: Color,
    isDecimal: Boolean = false,
    topPadding: androidx.compose.ui.unit.Dp = 6.dp
) {
    val progress = (current / target).coerceIn(0f, 1f)
    val totalBarWidth = 200.dp
    val filledWidth = totalBarWidth * progress

    val valueText = if (isDecimal) {
        "$label: %.1f/%.1f".format(current, target)
    } else {
        "$label: ${current.toInt()}/${target.toInt()}"
    }

    Column(modifier = GlanceModifier.padding(top = topPadding)) {
        Text(
            text = valueText,
            style = TextStyle(
                fontSize = 11.sp,
                color = ColorProvider(day = textColor, night = textColor)
            )
        )
        Box(
            modifier = GlanceModifier
                .width(totalBarWidth)
                .height(8.dp)
                .background(ColorProvider(day = color.copy(alpha = 0.2f), night = color.copy(alpha = 0.2f)))
        ) {
            Box(
                modifier = GlanceModifier
                    .width(filledWidth)
                    .height(8.dp)
                    .background(ColorProvider(day = color, night = color))
            ) {}
        }
    }
}