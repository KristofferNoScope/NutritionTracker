package com.example.nutritiontracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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