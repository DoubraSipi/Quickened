package com.quickened.content

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.quickened.data.Session
import java.io.File

// Formats sessions as plain text and shares via system sheet. Offline, no permissions.
object ExportHelper {
    fun format(sessions: List<Session>): String {
        if (sessions.isEmpty()) return "Quickened — no moments yet."
        return buildString {
            appendLine("Quickened — my moments")
            appendLine("======================")
            sessions.forEach { s ->
                appendLine()
                appendLine(s.contentPreview)
                appendLine("${s.tone} • ${s.activity} • ${s.durationSeconds}s • ${s.timestamp}")
                if (s.isFavorite) appendLine("★ Kept")
            }
        }
    }

    fun share(context: Context, sessions: List<Session>) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "quickened-moments.txt")
        file.writeText(format(sessions))
        val uri = FileProvider.getUriForFile(context, "com.quickened.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My Quickened moments")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share moments"))
    }
}
