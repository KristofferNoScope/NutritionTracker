package com.example.nutritiontracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun NutrientRing(
    label: String,
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val rawProgress = current.toFloat() / target.toFloat()
    val baseProgress = rawProgress.coerceIn(0f, 1f)
    val overflowProgress = (rawProgress - 1f).coerceIn(0f, 1f)
    val percent = (rawProgress * 100).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(64.dp)) {
                val strokeWidth = 6.dp.toPx()

                // Bakgrundsspår (den tomma delen av ringen)
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                // Grundframsteg (0–100%)
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = baseProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                // Överskott (allt över 100%), ritas i varningsfärg ovanpå
                if (overflowProgress > 0f) {
                    drawArc(
                        color = Color(0xFFD32F2F), // röd varningsfärg
                        startAngle = -90f,
                        sweepAngle = overflowProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}