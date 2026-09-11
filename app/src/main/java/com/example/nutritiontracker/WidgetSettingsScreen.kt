package com.example.nutritiontracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WidgetSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { WidgetSettingsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val savedTheme by repository.theme.collectAsState(initial = WidgetTheme.WHITE)
    var selectedTheme by remember(savedTheme) { mutableStateOf(savedTheme) }
    var isUpdating by remember { mutableStateOf(false) }

    fun selectTheme(theme: WidgetTheme) {
        if (isUpdating) return

        selectedTheme = theme
        isUpdating = true

        scope.launch {
            try {
                repository.saveTheme(theme)

                val manager = GlanceAppWidgetManager(context.applicationContext)
                val glanceIds = manager.getGlanceIds(NutritionWidget::class.java)

                glanceIds.forEach { glanceId ->
                    NutritionWidget().update(context.applicationContext, glanceId)
                }

                delay(1200)

                glanceIds.forEach { glanceId ->
                    NutritionWidget().update(context.applicationContext, glanceId)
                }

                delay(300)
            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdate", "Failed to update widget", e)
            } finally {
                isUpdating = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Tillbaka"
                )
            }
            Text("Widget Teman")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Välj bakgrund")

            Spacer(modifier = Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.alpha(if (isUpdating) 0.3f else 1f)
                ) {
                    ThemeOptionBox(
                        label = "Vitt",
                        boxColor = Color.White,
                        isSelected = selectedTheme == WidgetTheme.WHITE,
                        onClick = { selectTheme(WidgetTheme.WHITE) }
                    )
                    ThemeOptionBox(
                        label = "Svart",
                        boxColor = Color.Black,
                        isSelected = selectedTheme == WidgetTheme.BLACK,
                        onClick = { selectTheme(WidgetTheme.BLACK) }
                    )
                    ThemeOptionBox(
                        label = "Transparent",
                        boxColor = Color.Gray.copy(alpha = 0.3f),
                        isSelected = selectedTheme == WidgetTheme.TRANSPARENT,
                        onClick = { selectTheme(WidgetTheme.TRANSPARENT) }
                    )
                }

                if (isUpdating) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionBox(
    label: String,
    boxColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(boxColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onClick() }
        ) {}

        Spacer(modifier = Modifier.height(4.dp))
        Text(label)
    }
}