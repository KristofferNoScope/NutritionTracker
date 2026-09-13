package com.example.nutritiontracker

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Some public APIs (including the Swedish Food Agency's) wrap the actual data in an envelope
 * object alongside pagination links, e.g.:
 *   {"links": [{"href": ..., "rel": "next", ...}], "data": [{"Nummer": 123, "Namn": "..."}]}
 * without documenting the exact field names. Instead of hardcoding guessed field names, we
 * inspect all array-valued fields and pick the one that doesn't look like a link array
 * (link objects have an "href" key), preferring the largest candidate if several remain.
 */
object JsonHelpers {

    fun findDataArray(rawJson: String): JSONArray {
        return when (val root = JSONTokener(rawJson).nextValue()) {
            is JSONArray -> root
            is JSONObject -> {
                val candidates = root.keys().asSequence()
                    .mapNotNull { key -> (root.opt(key) as? JSONArray)?.let { key to it } }
                    .toList()

                if (candidates.isEmpty()) {
                    throw IllegalStateException(
                        "No array field found in JSON response. Top-level keys: ${root.keys().asSequence().toList()}"
                    )
                }

                val nonLinkCandidates = candidates.filterNot { (_, array) ->
                    array.length() > 0 && array.optJSONObject(0)?.has("href") == true
                }

                (nonLinkCandidates.ifEmpty { candidates })
                    .maxByOrNull { it.second.length() }!!
                    .second
            }
            else -> throw IllegalStateException("Unexpected JSON root type: ${root::class.simpleName}")
        }
    }
}