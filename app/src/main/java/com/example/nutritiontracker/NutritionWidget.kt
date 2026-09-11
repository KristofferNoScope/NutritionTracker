package com.example.nutritiontracker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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

class NutritionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        val repository = WidgetSettingsRepository(context)
        val theme = repository.theme.first()

        android.util.Log.d("WidgetUpdate", "Widget reading theme: $theme")

        val backgroundColor = when (theme) {
            WidgetTheme.WHITE -> Color.White
            WidgetTheme.BLACK -> Color.Black
            WidgetTheme.TRANSPARENT -> Color.Transparent
        }
        val textColor = when (theme) {
            WidgetTheme.WHITE -> Color.Black
            WidgetTheme.BLACK, WidgetTheme.TRANSPARENT -> Color.White
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(day = backgroundColor, night = backgroundColor))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Dagens gång",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(day = textColor, night = textColor)
                    )
                )

                ProgressBar(label = "Km", current = 3.2f, target = 5.0f, color = Color(0xFF9C27B0), textColor = textColor, isDecimal = true, topPadding = 4.dp)
                ProgressBar(label = "Kcal", current = 1200f, target = 2000f, color = Color(0xFF4CAF50), textColor = textColor, topPadding = 10.dp)
                ProgressBar(label = "Protein", current = 50f, target = 120f, color = Color(0xFF2196F3), textColor = textColor, topPadding = 6.dp)
                ProgressBar(label = "Kolhydrater", current = 80f, target = 250f, color = Color(0xFFFFC107), textColor = textColor, topPadding = 6.dp)
                ProgressBar(label = "Fett", current = 30f, target = 70f, color = Color(0xFFF44336), textColor = textColor, topPadding = 6.dp)
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