package com.quickened.content

import android.content.Context
import com.quickened.data.AppDatabase
import com.quickened.data.BibleVerse
import org.json.JSONArray

// Full-Bible library (KJV, 31,102 verses). Imported once into Room from the
// bundled asset, then every pull is a true random draw from the whole Bible.
// ASV/WEB full texts slot in as extra columns/files later; curated pool stays
// as fallback until import completes.
object BibleLibrary {
    suspend fun ensureImported(context: Context, db: AppDatabase): Boolean {
        if (db.bibleDao().count() > 0) return true
        return try {
            val json = context.assets.open("bible/kjv.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            val rows = ArrayList<BibleVerse>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                rows.add(
                    BibleVerse(
                        book = o.getString("b"),
                        chapter = o.getInt("c"),
                        verse = o.getInt("v"),
                        text = o.getString("t")
                    )
                )
            }
            db.bibleDao().insertAll(rows)
            db.bibleDao().count() > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun randomVerse(db: AppDatabase, maxLen: Int = 400): BibleVerse? =
        db.bibleDao().randomVerse(40, maxLen)

    suspend fun passage(db: AppDatabase, first: BibleVerse): String {
        val next = db.bibleDao().passage(first.book, first.chapter, first.verse, 2)
        return next.joinToString(" ") { it.text } +
            " (${first.book} ${first.chapter}:${first.verse}" +
            (if (next.size > 1) "-${next.last().verse}" else "") + ")"
    }
}
