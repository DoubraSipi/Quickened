package com.quickened.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickened.reader.BookReader

@Composable
fun ReaderScreen(
    book: BookReader.Book?,
    bookUri: String?,
    chapter: Int,
    playing: Boolean,
    statusLine: String,
    onPick: (Uri, String) -> Unit,
    onChapter: (Int) -> Unit,
    onPlayPage: (Int) -> Unit,
    pageIx: Int,
    onPageIx: (Int) -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { onPick(it, it.lastPathSegment ?: "book") }
    }
    var fontSizeSp by remember { mutableIntStateOf(18) }
    var showPartMenu by remember { mutableStateOf(false) }
    var pageMode by remember { mutableStateOf(true) }
    var fullScreen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!fullScreen) {
            DrawerTopBar(title = "Ebook Reader", onMenu = onMenu)
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!fullScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    book?.title ?: "Ebook & Audiobook Reader",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifHeadings),
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                BlendedOutlineButton(
                    onClick = { picker.launch(arrayOf("application/pdf", "application/epub+zip", "text/plain")) }
                ) {
                    Text(if (book == null) "Open File" else "Change File", fontSize = 14.sp)
                }
            }
            }

            if (book != null) {
                if (!fullScreen) {
                // Audio control bar
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BlendedButton(
                                onClick = onPauseResume,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (playing) "❚❚ Pause" else "▶ Listen", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                            BlendedOutlineButton(onClick = onStop, modifier = Modifier.weight(1f)) {
                                Text("■ Stop", fontSize = 15.sp)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Part selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { if (chapter > 0) onChapter(chapter - 1) },
                                    enabled = chapter > 0
                                ) {
                                    Text("◄ Prev", fontSize = 13.sp)
                                }

                                TextButton(onClick = { showPartMenu = true }) {
                                    Text("Part ${chapter + 1} / ${book.chapters.size} ▼", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                DropdownMenu(
                                    expanded = showPartMenu,
                                    onDismissRequest = { showPartMenu = false }
                                ) {
                                    book.chapters.indices.forEach { ix ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Part ${ix + 1} (${book.chapters[ix].length / 1000}k chars)",
                                                    fontWeight = if (ix == chapter) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                showPartMenu = false
                                                onChapter(ix)
                                            }
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { if (chapter + 1 < book.chapters.size) onChapter(chapter + 1) },
                                    enabled = chapter + 1 < book.chapters.size
                                ) {
                                    Text("Next ►", fontSize = 13.sp)
                                }
                            }

                            // Font size adjustment
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { if (fontSizeSp > 14) fontSizeSp -= 2 }) {
                                    Text("A-", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("${fontSizeSp}sp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                TextButton(onClick = { if (fontSizeSp < 28) fontSizeSp += 2 }) {
                                    Text("A+", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (statusLine.isNotBlank()) {
                            Text(statusLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                }

                if (!fullScreen) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    BlendedOutlineButton(
                        onClick = { pageMode = !pageMode },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (pageMode) "☰ Parts view" else "▤ Page view", fontSize = 15.sp) }
                }
                }

                if (pageMode) {
                    // Adobe-style view: rendered PDF pages, or paper text for EPUB/TXT.
                    val context = LocalContext.current
                    var pageBitmap by remember(bookUri, pageIx) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    var rendering by remember(bookUri, pageIx) { mutableStateOf(book.isPdf) }
                    var zoom by remember(bookUri, pageIx) { mutableFloatStateOf(1f) }
                    var pan by remember(bookUri, pageIx) { mutableStateOf(Offset.Zero) }
                    val zoomState = rememberTransformableState { zoomChange, panChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(1f, 5f)
                        if (zoom > 1f) pan += panChange else pan = Offset.Zero
                    }
                    LaunchedEffect(bookUri, pageIx) {
                        pageBitmap?.recycle()
                        pageBitmap = null
                        if (book.isPdf && bookUri != null) {
                            rendering = true
                            pageBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                // 2x width keeps zoomed pages crisp on low-end screens.
                                val width = (context.resources.displayMetrics.widthPixels * 2f).toInt().coerceAtMost(2400)
                                try {
                                    BookReader.renderPdfPage(
                                        context,
                                        android.net.Uri.parse(bookUri),
                                        pageIx,
                                        width
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            rendering = false
                        }
                    }
                    // Page toolbar: counter + zoom + fullscreen toggle
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Page ${pageIx + 1} / ${book.pages.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        if (book.isPdf) {
                            TextButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f); if (zoom == 1f) pan = Offset.Zero }) {
                                Text("A-", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text("${String.format("%.1f", zoom)}x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            TextButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(5f) }) {
                                Text("A+", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        BlendedOutlineButton(onClick = { fullScreen = !fullScreen }) {
                            Text(if (fullScreen) "⤢ Exit" else "⛶ Full", fontSize = 14.sp)
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(if (fullScreen) 0.dp else 12.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFFFDF5)),
                        modifier = if (fullScreen) Modifier.fillMaxSize().weight(1f)
                        else Modifier.fillMaxWidth().weight(1f)
                    ) {
                        if (rendering) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("Loading page…") }
                        } else if (pageBitmap != null) {
                            val bmp = pageBitmap
                            if (bmp != null) {
                                val img = bmp.asImageBitmap()
                                if (zoom <= 1f) {
                                    val vScroll = rememberScrollState()
                                    LaunchedEffect(bookUri, pageIx) { vScroll.scrollTo(0) }
                                    Box(
                                        modifier = Modifier.fillMaxSize().verticalScroll(vScroll),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Image(
                                            bitmap = img,
                                            contentDescription = "Page ${pageIx + 1}",
                                            modifier = Modifier.fillMaxWidth().transformable(zoomState),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().transformable(zoomState),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = img,
                                            contentDescription = "Page ${pageIx + 1} zoomed",
                                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                                scaleX = zoom,
                                                scaleY = zoom,
                                                translationX = pan.x,
                                                translationY = pan.y
                                            ),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        } else {
                            val pageText = if (pageIx in book.pages.indices) book.pages[pageIx] else ""
                            val pageScroll = rememberScrollState()
                            LaunchedEffect(bookUri, pageIx) {
                                pageScroll.scrollTo(0)
                            }
                            Column(
                            modifier = Modifier.fillMaxSize().padding(22.dp).verticalScroll(pageScroll),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Page ${pageIx + 1} of ${book.pages.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                pageText.ifBlank { "(blank page)" },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = SerifHeadings,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * 1.6).sp,
                                    color = androidx.compose.ui.graphics.Color(0xFF2B2620)
                                )
                            )
                        }
                    }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BlendedOutlineButton(
                            onClick = { if (pageIx > 0) onPageIx(pageIx - 1) },
                            modifier = Modifier.weight(1f)
                        ) { Text("◄ Prev", fontSize = 15.sp) }
                        BlendedButton(
                            onClick = { onPlayPage(pageIx) },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (playing) "❚❚ Pause" else "▶ Listen", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                        BlendedOutlineButton(
                            onClick = { if (pageIx + 1 < book.pages.size) onPageIx(pageIx + 1) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Next ►", fontSize = 15.sp) }
                    }
                } else {
                // Full Book Page View
                val currentText = if (chapter in book.chapters.indices) book.chapters[chapter] else ""
                val scrollState = rememberScrollState()

                LaunchedEffect(chapter) {
                    scrollState.scrollTo(0)
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Part ${chapter + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            currentText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = SerifHeadings,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.55).sp
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (chapter > 0) {
                                BlendedOutlineButton(onClick = { onChapter(chapter - 1) }) {
                                    Text("◄ Previous Part", fontSize = 14.sp)
                                }
                            } else {
                                Spacer(Modifier.height(1.dp))
                            }

                            if (chapter + 1 < book.chapters.size) {
                                BlendedButton(onClick = { onChapter(chapter + 1) }) {
                                    Text("Next Part ►", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                }
            } else {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Read & Listen Offline",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = SerifHeadings)
                        )
                        Text(
                            "Open PDF, EPUB, or TXT ebooks. You can read the text on screen while listening to offline voices.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        BlendedButton(
                            onClick = { picker.launch(arrayOf("application/pdf", "application/epub+zip", "text/plain")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📖 Choose Book File", fontSize = 16.sp)
                        }
                    }
                }
            }

            if (!fullScreen) {
                BlendedOutlineButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("← Home", fontSize = 15.sp) }
            }
        }
    }
}
