package com.example.nutritiontracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

                // Background track (the empty part of the ring)
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                // Base progress (0-100%)
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = baseProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                // Overflow (anything over 100%), drawn in a warning color on top
                if (overflowProgress > 0f) {
                    drawArc(
                        color = Color(0xFFD32F2F),
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

@Composable
fun StepProgressBar(
    currentKm: Float,
    targetKm: Float,
    modifier: Modifier = Modifier
) {
    val progress = (currentKm / targetKm).coerceIn(0f, 1f)
    val color = Color(0xFF4CAF50)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today's Steps",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "%.1f / %.1f km".format(currentKm, targetKm),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(12.dp)
                    .background(color, RoundedCornerShape(6.dp))
            )
        }
    }
}

/**
 * Draws a simple line chart of weight entries over time. Deliberately minimal (no external
 * charting library) since we only need to plot a handful of points on one axis.
 */
@Composable
fun WeightChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    if (entries.size < 2) {
        Text(
            "Log at least two days to see a trend chart.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val color = Color(0xFF9C27B0)
    val minWeight = entries.minOf { it.weightKg }
    val maxWeight = entries.maxOf { it.weightKg }
    // Avoid a flat/zero-height range when all entries have the same weight.
    val range = (maxWeight - minWeight).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val paddingPx = 16.dp.toPx()
        val chartWidth = size.width - paddingPx * 2
        val chartHeight = size.height - paddingPx * 2

        val points = entries.mapIndexed { index, entry ->
            val x = paddingPx + (chartWidth * index / (entries.size - 1).coerceAtLeast(1))
            val normalized = (entry.weightKg - minWeight) / range
            val y = paddingPx + chartHeight - (chartHeight * normalized)
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f
            )
        }

        points.forEach { point ->
            drawCircle(color = color, radius = 6f, center = point)
        }
    }
}