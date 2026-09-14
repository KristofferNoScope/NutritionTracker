package com.example.nutritiontracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TargetsSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { UserTargetsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(TargetMode.MANUAL) }

    var manualKcal by remember { mutableStateOf("2000") }
    var manualProtein by remember { mutableStateOf("120") }
    var manualFat by remember { mutableStateOf("70") }
    var manualCarbs by remember { mutableStateOf("250") }

    var weight by remember { mutableStateOf("75") }
    var height by remember { mutableStateOf("175") }
    var age by remember { mutableStateOf("25") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var activityLevel by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var goal by remember { mutableStateOf(Goal.MAINTAIN) }

    var isSaving by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    // Load whatever was previously saved (or the defaults) once, when the screen opens.
    LaunchedEffect(Unit) {
        val settings = repository.settings.first()
        mode = settings.mode
        manualKcal = settings.manualKcal.toString()
        manualProtein = settings.manualProteinG.toString()
        manualFat = settings.manualFatG.toString()
        manualCarbs = settings.manualCarbsG.toString()
        weight = settings.profile.weightKg.toString()
        height = settings.profile.heightCm.toString()
        age = settings.profile.age.toString()
        sex = settings.profile.sex
        activityLevel = settings.profile.activityLevel
        goal = settings.profile.goal
    }

    val calculatedPreview = remember(weight, height, age, sex, activityLevel, goal) {
        val w = weight.toFloatOrNull()
        val h = height.toFloatOrNull()
        val a = age.toIntOrNull()
        if (w != null && h != null && a != null) {
            TargetsCalculator.calculate(UserProfile(w, h, a, sex, activityLevel, goal))
        } else null
    }

    fun save() {
        val profile = UserProfile(
            weightKg = weight.toFloatOrNull() ?: 75f,
            heightCm = height.toFloatOrNull() ?: 175f,
            age = age.toIntOrNull() ?: 25,
            sex = sex,
            activityLevel = activityLevel,
            goal = goal
        )

        isSaving = true
        scope.launch {
            repository.save(
                mode = mode,
                manualKcal = manualKcal.toIntOrNull() ?: 2000,
                manualProteinG = manualProtein.toIntOrNull() ?: 120,
                manualFatG = manualFat.toIntOrNull() ?: 70,
                manualCarbsG = manualCarbs.toIntOrNull() ?: 250,
                profile = profile
            )
            updateNutritionWidgets(context.applicationContext)
            isSaving = false
            savedMessage = "Saved!"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Nutrition Goals", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Mode switch ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton("Manual", mode == TargetMode.MANUAL, Modifier.weight(1f)) {
                    mode = TargetMode.MANUAL
                }
                ModeButton("Calculated", mode == TargetMode.CALCULATED, Modifier.weight(1f)) {
                    mode = TargetMode.CALCULATED
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (mode == TargetMode.MANUAL) {
                Text("Enter your own daily targets", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                NumberField("Calories (kcal)", manualKcal) { manualKcal = it }
                NumberField("Protein (g)", manualProtein) { manualProtein = it }
                NumberField("Fat (g)", manualFat) { manualFat = it }
                NumberField("Carbs (g)", manualCarbs) { manualCarbs = it }
            } else {
                Text("Your stats", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                NumberField("Weight (kg)", weight) { weight = it }
                NumberField("Height (cm)", height) { height = it }
                NumberField("Age", age) { age = it }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Sex", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { option ->
                        ModeButton(option.label, sex == option, Modifier.weight(1f)) { sex = option }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Activity level", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    ActivityLevel.entries.forEach { option ->
                        SelectableRow(option.label, activityLevel == option) { activityLevel = option }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Goal", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach { option ->
                        ModeButton(option.label, goal == option, Modifier.weight(1f)) { goal = option }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                calculatedPreview?.let { preview ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Calculated targets", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${preview.kcal} kcal")
                            Text("${preview.proteinG} g protein")
                            Text("${preview.fatG} g fat")
                            Text("${preview.carbsG} g carbs")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { save() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }

            savedMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun ModeButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SelectableRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
    }
}