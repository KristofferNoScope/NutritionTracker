package com.example.nutritiontracker

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// One CSV file holds everything. The first column says what kind of row it is:
//   food      date, name, amount_g, kcal, protein_g, fat_g, carbs_g, food_number
//   weight    date, weight_kg
//   favorite  name, food_number
private const val CSV_HEADER =
    "type,date,name,amount_g,kcal,protein_g,fat_g,carbs_g,food_number,weight_kg"

// Foods in a file without a food number (for example one edited by hand) get the same
// placeholder as foods typed in manually.
private const val UNKNOWN_FOOD_NUMMER = -2

// A backup of years of logging is well under a megabyte. This only guards against picking the
// wrong file by mistake.
private const val MAX_BACKUP_BYTES = 20 * 1024 * 1024

data class BackupCounts(val food: Int, val weight: Int, val favorites: Int)

data class ImportSummary(
    val foodAdded: Int,
    val foodSkipped: Int,
    val weightAdded: Int,
    val weightSkipped: Int,
    val favoritesAdded: Int,
    val favoritesSkipped: Int
)

private class ParsedBackup(
    val food: List<FoodLogEntry>,
    val weights: List<WeightEntry>,
    val favorites: List<FavoriteFood>
)

// Identifies a log entry by its content. Ids are not part of the backup, since they only mean
// something inside one particular database.
private data class FoodKey(
    val date: String,
    val name: String,
    val grams: Float,
    val kcal: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val nummer: Int
)

private fun FoodLogEntry.key() =
    FoodKey(date, foodName, grams, kcal, protein, fat, carbs, foodNummer)

/**
 * Saves the food log, weight history and favorites to a CSV file, and reads such a file back.
 *
 * Importing only adds what is missing. Nothing is overwritten or deleted, so importing the same
 * file twice, or importing on top of existing data, is safe.
 */
class BackupRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val foodDao = db.foodDao()
    private val weightDao = db.weightDao()

    suspend fun exportTo(uri: Uri): BackupCounts {
        val food = foodDao.getAllLogEntries()
        val weights = weightDao.getAllEntries().first()
        val favorites = foodDao.getFavorites().first()

        val csv = buildCsv(food, weights, favorites)
        withContext(Dispatchers.IO) {
            // "wt" replaces the contents if the file already exists.
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw IllegalStateException("Couldn't open the file for writing.")
            stream.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
        }
        return BackupCounts(food.size, weights.size, favorites.size)
    }

    suspend fun importFrom(uri: Uri): ImportSummary {
        val text = withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Couldn't open the file.")
            if (bytes.size > MAX_BACKUP_BYTES) {
                throw IllegalArgumentException("This file is too large to be a NutritionTracker backup.")
            }
            String(bytes, Charsets.UTF_8)
        }

        // The whole file is checked before anything is saved, so a bad file changes nothing.
        val parsed = parseBackup(text)

        // Entries already in the app are matched one-to-one, so two identical meals on the same
        // day (a real case) are neither lost nor doubled.
        val remaining = foodDao.getAllLogEntries()
            .groupingBy { it.key() }.eachCount().toMutableMap()
        val foodToAdd = mutableListOf<FoodLogEntry>()
        var foodSkipped = 0
        for (entry in parsed.food) {
            val key = entry.key()
            val left = remaining[key] ?: 0
            if (left > 0) {
                remaining[key] = left - 1
                foodSkipped++
            } else {
                foodToAdd.add(entry)
            }
        }

        // Weight is one entry per day, and a day that already has one keeps it.
        val existingWeightDates = weightDao.getAllEntries().first().map { it.date }.toSet()
        val weightsToAdd = parsed.weights.filter { it.date !in existingWeightDates }

        val existingFavorites = foodDao.getFavorites().first().map { it.nummer }.toSet()
        val favoritesToAdd = parsed.favorites.filter { it.nummer !in existingFavorites }

        // All or nothing: if saving fails halfway, nothing is left half imported.
        db.withTransaction {
            foodDao.insertLogEntries(foodToAdd)
            weightsToAdd.forEach { weightDao.insert(it) }
            favoritesToAdd.forEach { foodDao.insertFavorite(it) }
        }

        return ImportSummary(
            foodAdded = foodToAdd.size,
            foodSkipped = foodSkipped,
            weightAdded = weightsToAdd.size,
            weightSkipped = parsed.weights.size - weightsToAdd.size,
            favoritesAdded = favoritesToAdd.size,
            favoritesSkipped = parsed.favorites.size - favoritesToAdd.size
        )
    }
}

// ---------- Writing ----------

private fun buildCsv(
    food: List<FoodLogEntry>,
    weights: List<WeightEntry>,
    favorites: List<FavoriteFood>
): String {
    val sb = StringBuilder()
    // A byte order mark makes Excel read the file as UTF-8, so å, ä and ö look right.
    sb.append('\uFEFF')
    sb.append(CSV_HEADER).append("\r\n")

    fun line(vararg fields: String) {
        sb.append(fields.joinToString(",") { csvField(it) }).append("\r\n")
    }

    // Float.toString() always uses a decimal point and gives back exactly the same number when
    // read again, whatever language the phone is set to.
    food.forEach { e ->
        line(
            "food", e.date, e.foodName, e.grams.toString(), e.kcal.toString(),
            e.protein.toString(), e.fat.toString(), e.carbs.toString(),
            e.foodNummer.toString(), ""
        )
    }
    weights.forEach { w ->
        line("weight", w.date, "", "", "", "", "", "", "", w.weightKg.toString())
    }
    favorites.forEach { f ->
        line("favorite", "", f.namn, "", "", "", "", "", f.nummer.toString(), "")
    }
    return sb.toString()
}

// Fields with a comma, quote or line break are wrapped in quotes, with inner quotes doubled.
private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

// ---------- Reading ----------

private fun parseBackup(rawText: String): ParsedBackup {
    val text = rawText.removePrefix("\uFEFF")

    // Files saved by Excel with Swedish settings use semicolons instead of commas.
    val firstLine = text.substringBefore('\n')
    val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','

    val rows = parseCsv(text, delimiter)
    if (rows.isEmpty()) throw IllegalArgumentException("The file is empty.")

    val header = rows.first().map { it.trim().lowercase() }
    val column = header.withIndex().associate { (index, name) -> name to index }
    if ("type" !in column) {
        throw IllegalArgumentException(
            "This doesn't look like a NutritionTracker backup: the \"type\" column is missing."
        )
    }

    val food = mutableListOf<FoodLogEntry>()
    val weights = mutableListOf<WeightEntry>()
    val favorites = mutableListOf<FavoriteFood>()

    rows.drop(1).forEachIndexed { index, row ->
        if (row.all { it.isBlank() }) return@forEachIndexed
        val rowNumber = index + 2 // the header is row 1, as in a spreadsheet

        fun field(name: String): String = column[name]?.let { row.getOrNull(it) }?.trim().orEmpty()
        fun fail(message: String): Nothing =
            throw IllegalArgumentException("Row $rowNumber: $message")

        // Accepts "5.5" and "5,5". An empty optional field counts as 0.
        fun number(name: String, required: Boolean = false): Float {
            val raw = field(name)
            if (raw.isEmpty()) {
                if (required) fail("\"$name\" is missing.")
                return 0f
            }
            val value = raw.replace(',', '.').toFloatOrNull()
            if (value == null || !value.isFinite() || value < 0f) {
                fail("\"$name\" must be a number of 0 or more, but is \"$raw\".")
            }
            return value
        }

        fun date(): String {
            val raw = field("date")
            val parts = raw.split("-")
            val valid = parts.size == 3 &&
                    parts[0].length == 4 && parts[1].length == 2 && parts[2].length == 2 &&
                    parts.all { part -> part.all { it in '0'..'9' } } &&
                    parts[1].toInt() in 1..12 && parts[2].toInt() in 1..31
            if (!valid) fail("\"date\" must look like 2026-09-21, but is \"$raw\".")
            return raw
        }

        when (field("type").lowercase()) {
            "food" -> {
                val name = field("name")
                if (name.isEmpty()) fail("\"name\" is missing.")
                val grams = number("amount_g", required = true)
                if (grams <= 0f) fail("\"amount_g\" must be above 0.")
                food.add(
                    FoodLogEntry(
                        foodNummer = field("food_number").toIntOrNull() ?: UNKNOWN_FOOD_NUMMER,
                        foodName = name,
                        grams = grams,
                        date = date(),
                        kcal = number("kcal", required = true),
                        protein = number("protein_g"),
                        fat = number("fat_g"),
                        carbs = number("carbs_g")
                    )
                )
            }
            "weight" -> {
                val kg = number("weight_kg", required = true)
                if (kg <= 0f) fail("\"weight_kg\" must be above 0.")
                weights.add(WeightEntry(date = date(), weightKg = kg))
            }
            "favorite" -> {
                val name = field("name")
                val nummer = field("food_number").toIntOrNull()
                if (name.isEmpty() || nummer == null) {
                    fail("a favorite needs both a \"name\" and a \"food_number\".")
                }
                favorites.add(FavoriteFood(nummer = nummer, namn = name))
            }
            else -> fail(
                "unknown type \"${field("type")}\" (expected food, weight or favorite)."
            )
        }
    }

    // If a file has the same day or favorite twice, the first one is used.
    return ParsedBackup(
        food = food,
        weights = weights.distinctBy { it.date },
        favorites = favorites.distinctBy { it.nummer }
    )
}

/** Splits CSV text into rows of fields. Handles quoted fields with commas and line breaks. */
private fun parseCsv(text: String, delimiter: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false

    fun endField() {
        row.add(field.toString())
        field.clear()
    }

    fun endRow() {
        endField()
        rows.add(row)
        row = mutableListOf()
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    field.append('"') // a doubled quote inside quotes is one literal quote
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                field.append(c)
            }
        } else {
            when (c) {
                '"' -> if (field.isEmpty()) inQuotes = true else field.append(c)
                delimiter -> endField()
                '\r' -> Unit // the \n that follows ends the row
                '\n' -> endRow()
                else -> field.append(c)
            }
        }
        i++
    }
    // The last row may not end with a line break.
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}