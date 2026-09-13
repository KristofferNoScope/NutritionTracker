package com.example.nutritiontracker

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Some public APIs (including the Swedish Food Agency's) wrap a JSON array in an envelope
 * object (e.g. {"data": [...], "totalCount": 123}) without documenting the exact field name.
 * Instead of hardcoding a guessed field name, we parse the root value and, if it's an object,
 * find the first property whose value is a JSON array.
 */
object JsonHelpers {

    fun findFirstJsonArray(rawJson: String): JSONArray {
        return when (val root = JSONTokener(rawJson).nextValue()) {
            is JSONArray -> root
            is JSONObject -> {
                val key = root.keys().asSequence().firstOrNull { root.get(it) is JSONArray }
                    ?: throw IllegalStateException(
                        "No array field found in JSON response. Top-level keys: ${root.keys().asSequence().toList()}"
                    )
                root.getJSONArray(key)
            }
            else -> throw IllegalStateException("Unexpected JSON root type: ${root::class.simpleName}")
        }
    }
}