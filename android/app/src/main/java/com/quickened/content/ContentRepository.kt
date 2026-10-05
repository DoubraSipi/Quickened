package com.quickened.content

import android.content.Context
import org.json.JSONArray

data class Reflection(val title: String, val text: String, val type: String = "reflection")
// type: "verse" (pure Scripture) | "reflection" (verse + short comment)

// Loads local assets/content/<tone>.json. No network. Falls back to stub if missing.
object ContentRepository {
    fun random(context: Context, tone: String): Reflection {
        val list = load(context, tone)
        if (list.isEmpty()) return Reflection("Quiet moment", "Be still, and know that I am God.", "verse")
        return list.random()
    }

    fun load(context: Context, tone: String): List<Reflection> {
        return try {
            val json = context.assets.open("content/$tone.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Reflection(
                    o.getString("title"),
                    o.getString("text"),
                    o.optString("type", "reflection")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
