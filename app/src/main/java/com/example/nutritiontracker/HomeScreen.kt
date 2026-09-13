package com.example.nutritiontracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onLogFoodClick: () -> Unit,
    onWidgetSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FoodRepository(context.applicationContext) }
    val today = remember { todayDateString() }

    val todaysEntries by repository.getLogEntriesForDate(today).collectAsState(initial = emptyList())

    val totalKcal = todaysEntries.sumOf { it.kcal.toDouble() }.roundToInt()
    val totalProtein = todaysEntries.sumOf { it.protein.toDouble() }.roundToInt()
    val totalFat = todaysEntries.sumOf { it.fat.toDouble() }.roundToInt()
    val totalCarbs = todaysEntries.sumOf { it.carbs.toDouble() }.roundToInt()

    // Step counter sensors require the ACTIVITY_RECOGNITION runtime permission on Android 10+.
    // On older versions no runtime permission is needed, so we treat it as already granted.
    var hasStepsPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStepsPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasStepsPermission) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    val todaySteps = rememberTodaySteps(enabled = hasStepsPermission)
    val currentKm = todaySteps * STRIDE_LENGTH_KM

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StepProgressBar(
            currentKm = currentKm,
            targetKm = DAILY_KM_TARGET,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutrientRing(
                label = "Kcal",
                current = totalKcal,
                target = DAILY_KCAL_TARGET,
                color = Color(0xFF4CAF50)
            )
            NutrientRing(
                label = "Protein",
                current = totalProtein,
                target = DAILY_PROTEIN_TARGET_G,
                color = Color(0xFF2196F3)
            )
            NutrientRing(
                label = "Carbs",
                current = totalCarbs,
                target = DAILY_CARBS_TARGET_G,
                color = Color(0xFFFFC107)
            )
            NutrientRing(
                label = "Fat",
                current = totalFat,
                target = DAILY_FAT_TARGET_G,
                color = Color(0xFFF44336)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onLogFoodClick) {
            Text("Log Food")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onWidgetSettingsClick) {
            Text("Widget Theme")
        }
    }
}