package com.example.nutritiontracker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { BackupRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun runExport(uri: Uri) {
        isWorking = true
        message = null
        scope.launch {
            try {
                val counts = repository.exportTo(uri)
                isError = false
                message = "Backup saved: ${counts.food} food entries, " +
                        "${counts.weight} weight entries and ${counts.favorites} favorites."
            } catch (e: Exception) {
                android.util.Log.e("Backup", "Export failed", e)
                isError = true
                message = "Couldn't save the backup: ${e.message}"
            } finally {
                isWorking = false
            }
        }
    }

    fun runImport(uri: Uri) {
        isWorking = true
        message = null
        scope.launch {
            try {
                val result = repository.importFrom(uri)
                // The widget shows today's totals, which may have changed.
                updateNutritionWidgets(context.applicationContext)
                isError = false
                message = "Import finished. Added ${result.foodAdded} food entries " +
                        "(${result.foodSkipped} already existed), " +
                        "${result.weightAdded} weight entries " +
                        "(${result.weightSkipped} already existed) and " +
                        "${result.favoritesAdded} favorites " +
                        "(${result.favoritesSkipped} already existed)."
            } catch (e: Exception) {
                android.util.Log.e("Backup", "Import failed", e)
                isError = true
                message = "Couldn't import this file: ${e.message}"
            } finally {
                isWorking = false
            }
        }
    }

    // The system file picker is used, so the app needs no storage permission. The user chooses
    // where to save the file (Downloads, Google Drive, ...) and which file to read back.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) runExport(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) runImport(uri)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Backup", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Export", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Saves your food log, weight history and favorites to a CSV file. You choose " +
                        "where to keep it, for example in Downloads or on Google Drive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { exportLauncher.launch("nutritiontracker-backup-${todayDateString()}.csv") },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export to CSV")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Import", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Reads a backup file made by this app and adds whatever is missing. Nothing " +
                        "is overwritten or deleted, so importing the same file twice is safe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import from CSV")
            }

            message?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    it,
                    color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Your nutrition goals, step goal and widget theme are not included in the " +
                        "backup and need to be set again after a reinstall.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}