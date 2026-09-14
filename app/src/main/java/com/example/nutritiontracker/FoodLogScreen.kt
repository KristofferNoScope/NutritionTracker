package com.example.nutritiontracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FoodLogScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { FoodRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(todayDateString()) }
    val isToday = selectedDate == todayDateString()

    var isCacheLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var gramsInput by remember { mutableStateOf("") }
    var isLogging by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var editingEntry by remember { mutableStateOf<FoodLogEntry?>(null) }
    var editGramsInput by remember { mutableStateOf("") }

    val dayEntries by repository.getLogEntriesForDate(selectedDate).collectAsState(initial = emptyList())
    val favorites by repository.getFavorites().collectAsState(initial = emptyList())

    // Load the food item cache once, the first time this screen is opened.
    LaunchedEffect(Unit) {
        try {
            repository.ensureFoodCacheLoaded()
        } catch (e: Exception) {
            android.util.Log.e("FoodLog", "Failed to load food cache", e)
            errorMessage = "Couldn't load the food database: ${e.message}"
        } finally {
            isCacheLoading = false
        }
    }

    // Debounced search: waits briefly after the user stops typing before querying Room.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        searchResults = repository.searchFoods(searchQuery)
        isSearching = false
    }

    fun confirmLog() {
        val food = selectedFood ?: return
        val grams = gramsInput.toFloatOrNull()
        if (grams == null || grams <= 0f) {
            errorMessage = "Enter a valid amount in grams."
            return
        }

        isLogging = true
        errorMessage = null

        scope.launch {
            try {
                val per100g = repository.getNutrientsPer100g(food.nummer)
                repository.logFood(
                    foodNummer = food.nummer,
                    foodName = food.namn,
                    grams = grams,
                    date = selectedDate,
                    per100g = per100g
                )
                selectedFood = null
                gramsInput = ""
                searchQuery = ""
                searchResults = emptyList()
                updateNutritionWidgets(context.applicationContext)
            } catch (e: Exception) {
                android.util.Log.e("FoodLog", "Failed to log food", e)
                errorMessage = "Couldn't log this food: ${e.message}"
            } finally {
                isLogging = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text("Log Food", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (isCacheLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Loading food database...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedFood = null
                    },
                    label = { Text("Search food (Swedish)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Show favorites for quick access when the user hasn't typed a search yet.
                if (searchQuery.isBlank() && selectedFood == null && favorites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Favorites", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(favorites) { favorite ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFood = FoodItem(favorite.nummer, favorite.namn)
                                        errorMessage = null
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(favorite.namn, modifier = Modifier.padding(start = 8.dp))
                            }
                            HorizontalDivider()
                        }
                    }
                }

                if (selectedFood == null && searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(searchResults) { food ->
                            Text(
                                text = food.namn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .clickable {
                                        selectedFood = food
                                        searchResults = emptyList()
                                        errorMessage = null
                                    }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                selectedFood?.let { food ->
                    val isFavorite by repository.isFavorite(food.nummer).collectAsState(initial = false)

                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    food.namn,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    scope.launch {
                                        if (isFavorite) {
                                            repository.removeFavorite(food.nummer)
                                        } else {
                                            repository.addFavorite(food.nummer, food.namn)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    selectedFood = null
                                    gramsInput = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = gramsInput,
                                onValueChange = { gramsInput = it },
                                label = { Text("Amount (grams)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { confirmLog() },
                                enabled = !isLogging,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLogging) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Log")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Date navigation: browse to previous/next days.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedDate = shiftDate(selectedDate, -1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                    Text(formatDateForDisplay(selectedDate), style = MaterialTheme.typography.titleSmall)
                    IconButton(
                        onClick = { selectedDate = shiftDate(selectedDate, 1) },
                        enabled = !isToday
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (dayEntries.isEmpty()) {
                    Text(
                        "Nothing logged for this day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(dayEntries) { entry ->
                            FoodLogEntryRow(
                                entry = entry,
                                onEdit = {
                                    editingEntry = entry
                                    editGramsInput = entry.grams.toString()
                                },
                                onDelete = {
                                    scope.launch {
                                        repository.deleteLogEntry(entry)
                                        updateNutritionWidgets(context.applicationContext)
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    editingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text("Edit amount") },
            text = {
                Column {
                    Text(entry.foodName, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editGramsInput,
                        onValueChange = { editGramsInput = it },
                        label = { Text("Amount (grams)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newGrams = editGramsInput.toFloatOrNull()
                    if (newGrams != null && newGrams > 0f) {
                        scope.launch {
                            repository.updateLogEntryGrams(entry, newGrams)
                            updateNutritionWidgets(context.applicationContext)
                        }
                        editingEntry = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FoodLogEntryRow(entry: FoodLogEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.foodName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "%.0f g · %.0f kcal · P %.1f  F %.1f  C %.1f".format(
                    entry.grams, entry.kcal, entry.protein, entry.fat, entry.carbs
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit amount")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}