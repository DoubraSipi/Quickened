package com.quickened.reader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import nl.siegmann.epublib.epub.EpubReader

// Audiobook reader core: PDF / EPUB / TXT -> plain-text chapters.
// DOCX excluded by design (heavy POI dependency). Offline, no permissions
// (caller supplies a Storage-Access-Framework Uri).
object BookReader {

    data class Book(val title: String, val chapters: List<String>, val pages: List<String> = chapters, val isPdf: Boolean = false)

    fun open(context: Context, uri: Uri, name: String): Book {
        val displayName = queryDisplayName(context, uri) ?: name.ifBlank { "book" }
        val mime = try { context.contentResolver.getType(uri) ?: "" } catch (e: Exception) { "" }
        val lowerName = displayName.lowercase()
        val lowerMime = mime.lowercase()
        val wantPdf = lowerMime.contains("pdf") || lowerName.endsWith(".pdf")
        val wantEpub = lowerMime.contains("epub") || lowerName.endsWith(".epub")
        val wantText = lowerMime.startsWith("text/") || lowerName.endsWith(".txt")
        return when {
            wantPdf -> openPdf(context, uri, displayName)
            wantEpub -> openEpub(context, uri, displayName)
            wantText -> openText(context, uri, displayName)
            else -> {
                // Document-provider Uris (msf:, document IDs) carry no extension:
                // sniff content instead of showing raw binary as "code".
                try {
                    openPdf(context, uri, displayName)
                } catch (e1: Exception) {
                    try {
                        openEpub(context, uri, displayName)
                    } catch (e2: Exception) {
                        openText(context, uri, displayName)
                    }
                }
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun openPdf(context: Context, uri: Uri, name: String): Book {
        PDFBoxResourceLoader.init(context.applicationContext)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "cannot open file" }
            PDDocument.load(input).use { doc ->
                val stripper = PDFTextStripper()
                val chapters = ArrayList<String>()
                val pages = ArrayList<String>()
                val pageCount = doc.numberOfPages
                var i = 1
                while (i <= pageCount) {
                    // 10-page sections keep spoken chunks manageable.
                    val end = (i + 9).coerceAtMost(pageCount)
                    stripper.startPage = i
                    stripper.endPage = end
                    val section = stripper.getText(doc).trim()
                    if (section.isNotEmpty()) chapters.add(section)
                    var p = i
                    while (p <= end) {
                        stripper.startPage = p
                        stripper.endPage = p
                        pages.add(stripper.getText(doc).trim())
                        p++
                    }
                    i = end + 1
                }
                if (chapters.isEmpty()) throw IllegalStateException("no readable text (scanned PDF?)")
                return Book(titleOf(name), chapters, pages, isPdf = true)
            }
        }
    }

    private fun openEpub(context: Context, uri: Uri, name: String): Book {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "cannot open file" }
            val book = EpubReader().readEpub(input)
            val chapters = book.contents.mapNotNull { res ->
                try {
                    val html = res.data.inputStream().bufferedReader().use { it.readText() }
                    val text = html
                        .replace(Regex("<script.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
                        .replace(Regex("<style.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
                        .replace(Regex("<[^>]+>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    if (text.length > 200) text else null
                } catch (e: Exception) {
                    null
                }
            }
            if (chapters.isEmpty()) throw IllegalStateException("no readable chapters")
            return Book(titleOf(name), chapters)
        }
    }

    private fun openText(context: Context, uri: Uri, name: String): Book {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "cannot open file" }
            val text = input.bufferedReader().use { it.readText() }.trim()
            if (text.isEmpty()) throw IllegalStateException("empty file")
            // ~3k-char spoken sections.
            val chunks = ArrayList<String>()
            var start = 0
            while (start < text.length) {
                val end = (start + 3000).coerceAtMost(text.length)
                chunks.add(text.substring(start, end))
                start = end
            }
            return Book(titleOf(name), chunks)
        }
    }

    // Renders a PDF page to a bitmap with Android's built-in renderer (offline).
    // Open/close per call: simple and leak-safe; ~0.5s per page on low-end devices.
    fun renderPdfPage(context: Context, uri: Uri, page: Int, targetWidthPx: Int): android.graphics.Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                android.graphics.pdf.PdfRenderer(fd).use { renderer ->
                    if (page !in 0 until renderer.pageCount) return null
                    renderer.openPage(page).use { pg ->
                        val scale = targetWidthPx.toFloat() / pg.width.toFloat()
                        val w = targetWidthPx
                        val h = (pg.height * scale).toInt().coerceAtLeast(1)
                        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        pg.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun titleOf(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        return if ("." in base) base.substringBeforeLast(".") else base
    }
}
