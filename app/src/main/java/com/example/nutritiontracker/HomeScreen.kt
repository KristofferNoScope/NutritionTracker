package com.example.nutritiontracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun HomeScreen(
    onLogFoodClick: () -> Unit,
    onWidgetSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StepProgressBar(
            currentKm = 3.2f,
            targetKm = 5.0f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutrientRing(label = "Kcal", current = 1200, target = 2000, color = Color(0xFF4CAF50))
            NutrientRing(label = "Protein", current = 50, target = 120, color = Color(0xFF2196F3))
            NutrientRing(label = "Kolhydrater", current = 80, target = 250, color = Color(0xFFFFC107))
            NutrientRing(label = "Fett", current = 30, target = 70, color = Color(0xFFF44336))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onLogFoodClick) {
            Text("Log Food")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onWidgetSettingsClick) {
            Text("Widget Tema")
        }
    }
}