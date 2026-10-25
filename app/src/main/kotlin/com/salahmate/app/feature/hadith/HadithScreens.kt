package com.salahmate.app.feature.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.core.design.arabicStyle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.EmptyState
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.components.LoadingState
import com.salahmate.app.core.design.components.StatusPill
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.hadith.Hadith
import com.salahmate.app.data.hadith.HadithMeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithLibraryScreen(viewModel: HadithViewModel, settings: UserSettings, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val open by viewModel.openCollection.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (open != null) {
        HadithReaderScreen(
            viewModel = viewModel,
            settings = settings,
            onBack = { viewModel.close() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "হাদিস সংকলন", "Hadith collections")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (ui.loading) {
            LoadingState(message = t(bn, "সংকলন লোড হচ্ছে…", "Loading collections…"))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                InfoBanner(
                    text = t(
                        bn,
                        "প্রতিটি হাদিসের সাথে সংকলন, বই ও হাদিস নম্বর দেওয়া আছে। গ্রেডিং শুধু উৎসে থাকলে দেখানো হয় এবং স্কলারের নাম উল্লেখ থাকে — অ্যাপ নিজে কোনো গ্রেডিং তৈরি করে না।",
                        "Every narration shows its collection, book and number. Grading appears only where the source supplies it, with the scholar named — the app never invents a grading."
                    ),
                    icon = Icons.Outlined.Info
                )
            }

            item { SectionLabel(t(bn, "অফলাইনে সংযুক্ত", "Included offline")) }
            items(ui.manifest.bundled, key = { it.id }) { meta ->
                CollectionCard(
                    meta = meta,
                    bangla = bn,
                    downloaded = true,
                    busy = false,
                    onOpen = { viewModel.open(meta) },
                    onDownload = {},
                    onDelete = {}
                )
            }

            if (ui.manifest.downloadable.isNotEmpty()) {
                item { SectionLabel(t(bn, "ডাউনলোডযোগ্য সংকলন", "Optional downloads")) }
                items(ui.manifest.downloadable, key = { it.id }) { meta ->
                    CollectionCard(
                        meta = meta,
                        bangla = bn,
                        downloaded = ui.downloadedIds.contains(meta.id),
                        busy = ui.busyCollection == meta.id,
                        onOpen = { viewModel.open(meta) },
                        onDownload = { viewModel.download(meta) },
                        onDelete = { viewModel.delete(meta) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun CollectionCard(
    meta: HadithMeta,
    bangla: Boolean,
    downloaded: Boolean,
    busy: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = Modifier.clickable(enabled = downloaded, onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meta.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${meta.hadithCount} " + t(bangla, "হাদিস", "narrations") + " · " + meta.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (downloaded) {
                        StatusPill(text = t(bangla, "অফলাইন", "Offline"))
                    } else {
                        StatusPill(text = t(bangla, "ডাউনলোড হয়নি", "Not downloaded"), color = MaterialTheme.colorScheme.outline)
                    }
                    StatusPill(text = meta.license, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
            } else if (downloaded && meta.id != "nawawi" && meta.id != "qudsi") {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = t(bangla, "মুছুন", "Remove download"), modifier = Modifier.size(20.dp))
                }
            } else if (!downloaded) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = t(bangla, "ডাউনলোড", "Download"), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithReaderScreen(viewModel: HadithViewModel, settings: UserSettings, onBack: () -> Unit) {
    val collection by viewModel.openCollection.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    val data = collection ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(data.collection.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${data.hadiths.size} " + t(bn, "হাদিস", "narrations"),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(data.hadiths, key = { it.number }) { hadith ->
                HadithCard(
                    hadith = hadith,
                    body = viewModel.body(hadith, settings),
                    bangla = bn,
                    showArabic = true,
                    bookmarked = ui.bookmarkedNumbers.contains(hadith.number),
                    onBookmark = { viewModel.toggleBookmark(hadith, data.collection.id) }
                )
            }
            item {
                Text(
                    t(
                        bn,
                        "উৎস: ${data.collection.source} · লাইসেন্স: ${data.collection.license}",
                        "Source: ${data.collection.source} · License: ${data.collection.license}"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HadithCard(
    hadith: Hadith,
    body: String,
    bangla: Boolean,
    showArabic: Boolean,
    bookmarked: Boolean,
    onBookmark: () -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${hadith.number}" + if (hadith.bookName.isNotBlank()) " · ${hadith.bookName}" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onBookmark, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (bookmarked) "Remove bookmark" else "Bookmark this narration",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (showArabic && hadith.arabic.isNotBlank()) {
            Text(
                hadith.arabic,
                style = arabicStyle(sizeSp = 22f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(body, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            t(bangla, "সূত্র: ", "Reference: ") + "book ${hadith.reference.book}, hadith ${hadith.reference.hadith}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hadith.grades.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                hadith.grades.forEach { grade ->
                    val strong = grade.grade.lowercase().let { it.contains("sahih") || it.contains("hasan") }
                    StatusPill(
                        text = grade.grade + if (grade.scholar.isNotBlank()) " — ${grade.scholar}" else "",
                        color = if (strong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/** Reusable list of bookmarked narrations across collections ("saved" surface). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkedHadithScreen(viewModel: HadithViewModel, settings: UserSettings, onBack: () -> Unit) {
    val open by viewModel.openCollection.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "সংরক্ষিত হাদিস", "Saved narrations")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        val collection = open
        if (collection == null) {
            EmptyState(
                title = t(bn, "কোনো সংকলন খোলা নেই", "No collection open"),
                message = t(bn, "সংকলন খুলে হাদিস বুকমার্ক করুন।", "Open a collection and bookmark narrations."),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(collection.hadiths.filter { ui.bookmarkedNumbers.contains(it.number) }) { hadith ->
                    HadithCard(
                        hadith = hadith,
                        body = viewModel.body(hadith, settings),
                        bangla = bn,
                        showArabic = true,
                        bookmarked = true,
                        onBookmark = { viewModel.toggleBookmark(hadith, collection.collection.id) }
                    )
                }
            }
        }
    }
}
