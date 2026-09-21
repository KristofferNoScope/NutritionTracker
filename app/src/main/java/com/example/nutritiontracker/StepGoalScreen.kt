package com.example.nutritiontracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val MAX_STEP_GOAL_KM = 100f

@Composable
fun StepGoalScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { UserTargetsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var goalText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Load the saved goal (or the default) once, when the screen opens.
    LaunchedEffect(Unit) {
        goalText = repository.getStepGoalKmOnce().toString()
    }

    fun save() {
        // Accept "5,5" as well as "5.5", since many keyboards use a decimal comma.
        val km = goalText.replace(',', '.').toFloatOrNull()
        if (km == null || km <= 0f || km > MAX_STEP_GOAL_KM) {
            isError = true
            message = "Enter a distance above 0 and up to ${MAX_STEP_GOAL_KM.toInt()} km."
            return
        }

        isSaving = true
        scope.launch {
            repository.saveStepGoalKm(km)
            updateNutritionWidgets(context.applicationContext)
            isSaving = false
            isError = false
            message = "Saved!"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Daily Step Goal", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = goalText,
                onValueChange = {
                    goalText = it
                    message = null
                },
                label = { Text("Distance (km)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Your step count is converted to an estimated distance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { save() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }

            message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    it,
                    color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}