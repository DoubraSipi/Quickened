package com.quickened.content

import android.content.Context
import org.json.JSONArray

data class Reflection(val title: String, val text: String, val type: String = "reflection", val alt: Map<String, String> = emptyMap())
// "Hebrews 12:1-2" -> "Hebrews chapter 12, verses 1 to 2" so TTS speaks it naturally.
// Leading book numbers become ordinals: "1 John" -> "First John".
fun spokenRef(text: String): String {
    return Regex("\\(([1-3]?\\s?[A-Za-z ]+?)\\s+(\\d+):(\\d+)(?:-(\\d+))?(?:,\\s*[A-Za-z]+)?\\)").replace(text) { m ->
        var book = m.groupValues[1].trim()
        book = Regex("^([123])\\s+(.+)$").replace(book) { b ->
            val ord = when (b.groupValues[1]) {
                "1" -> "First"
                "2" -> "Second"
                else -> "Third"
            }
            "$ord ${b.groupValues[2]}"
        }
        val ch = m.groupValues[2]
        val v1 = m.groupValues[3]
        val v2 = m.groupValues[4]
        if (v2.isNotEmpty()) "$book chapter $ch, verses $v1 to $v2"
        else "$book chapter $ch, verse $v1"
    }
} // "Hebrews 12:1-2" -> "Hebrews chapter 12, verses 1 to 2" so TTS speaks it naturally.

// type: "verse" (pure Scripture) | "reflection" (verse + short comment) | "fresh" (composed)
// alt: keyed alternates, e.g. "kjv" -> KJV wording. WEB can drop in later data-only.

// translation: "simple" (default text) | "kjv" (KJV wording for verses that carry it).
// Loads local assets/content/<tone>.json. No network. Falls back to stub if missing.
object ContentRepository {
    fun random(
        context: Context,
        tone: String,
        translation: String = "simple",
        excludeTitles: Set<String> = emptySet()
    ): Reflection {
        val list = load(context, tone, translation)
        if (list.isEmpty()) return Reflection("Quiet moment", "Be still, and know that I am God.", "verse")
        val fresh = list.filter { it.title !in excludeTitles }
        return (if (fresh.isNotEmpty()) fresh else list).random()
    }

    fun load(context: Context, tone: String, translation: String = "simple"): List<Reflection> {
        return try {
            val json = context.assets.open("content/$tone.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val type = o.optString("type", "reflection")
                val text = o.getString("text")
                val alt = buildMap {
                    if (o.has("kjv")) put("kjv", o.getString("kjv"))
                    if (o.has("web")) put("web", o.getString("web"))
                    if (o.has("asv")) put("asv", o.getString("asv"))
                }
                val resolved = if (translation != "simple" && type == "verse") alt[translation] ?: text else text
                Reflection(o.getString("title"), resolved, type, alt)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
