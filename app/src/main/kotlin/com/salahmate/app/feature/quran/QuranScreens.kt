package com.salahmate.app.feature.quran

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.Routes
import com.salahmate.app.core.design.arabicStyle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.EmptyState
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.components.LoadingState
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.quran.Ayah
import com.salahmate.app.data.quran.QuranEditions
import com.salahmate.app.data.quran.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    viewModel: QuranViewModel,
    settings: UserSettings,
    onOpenSurah: (Int, Int?) -> Unit
) {
    val state by viewModel.list.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    val filtered = viewModel.filteredSurahs()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(t(bn, "কুরআন", "Quran")) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val lastSurah = state.lastReadSurah
            val lastAyah = state.lastReadAyah
            if (lastSurah != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onOpenSurah(lastSurah, lastAyah) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t(bn, "শেষ পড়া অবস্থান", "Last read"), style = MaterialTheme.typography.labelSmall)
                            Text(
                                t(bn, "সূরা $lastSurah · আয়াত $lastAyah", "Surah $lastSurah · ayah $lastAyah"),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(t(bn, "চালিয়ে যান", "Continue"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onSearch,
                label = { Text(t(bn, "সূরা খুঁজুন", "Search surahs")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (state.loading) {
                LoadingState(message = t(bn, "কুরআন লোড হচ্ছে…", "Loading Quran…"))
            } else if (state.error != null) {
                EmptyState(
                    title = t(bn, "লোড করা যায়নি", "Could not load"),
                    message = state.error ?: "",
                    icon = Icons.Outlined.ErrorOutline
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.number }) { surah ->
                        SurahRow(
                            surah = surah,
                            bangla = bn,
                            onClick = { onOpenSurah(surah.number, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, bangla: Boolean, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Text("${surah.number}", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (bangla) surah.nameBengali else surah.nameEnglish,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${surah.nameEnglish} · ${surah.ayahCount} " + t(bangla, "আয়াত", "ayahs") + " · " +
                        t(bangla, "পারা", "Juz"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                surah.nameArabic,
                style = arabicStyle(sizeSp = 18f),
                textAlign = TextAlign.End
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    viewModel: QuranViewModel,
    settings: UserSettings,
    surahNumber: Int,
    startAyah: Int?,
    onBack: () -> Unit
) {
    val state by viewModel.reader.collectAsStateWithLifecycle()
    val audio by viewModel.audioState.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    var showDownloadDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(surahNumber) {
        viewModel.openSurah(surahNumber, startAyah, settings)
    }
    LaunchedEffect(state.surah?.number, startAyah) {
        val surah = state.surah ?: return@LaunchedEffect
        if (startAyah != null && startAyah in 1..surah.ayahCount) {
            listState.scrollToItem(startAyah.coerceAtMost(listState.layoutInfo.totalItemsCount.coerceAtLeast(0)))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.surah?.let { if (bn) it.nameBengali else it.nameEnglish } ?: t(bn, "সূরা", "Surah"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        state.surah?.let {
                            Text(
                                "${it.nameEnglish} · ${it.ayahCount} " + t(bn, "আয়াত", "ayahs"),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = { showDownloadDialog = true }) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = t(bn, "অফলাইনে ডাউনলোড", "Download offline"))
                    }
                }
            )
        },
        bottomBar = {
            if (state.surah != null) {
                AudioBar(
                    state = audio,
                    bangla = bn,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::nextAyah,
                    onPrevious = viewModel::previousAyah,
                    onSeek = viewModel::seekTo,
                    onPlayFromCurrent = { ayah -> viewModel.playAyah(state.surah!!, ayah, settings) }
                )
            }
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> EmptyState(
                title = t(bn, "সূরা লোড হয়নি", "Surah unavailable"),
                message = state.error ?: "",
                icon = Icons.Outlined.ErrorOutline,
                modifier = Modifier.padding(padding)
            )
            else -> {
                val surah = state.surah ?: return@Scaffold
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                surah.nameArabic,
                                style = arabicStyle(sizeSp = 26f, bold = true),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t(bn, "মক্কী/মাদানী: ", "Revelation: ") + surah.revelationType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (surah.number != 1 && surah.number != 9) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                    style = arabicStyle(sizeSp = 22f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (state.translationError != null && settings.showTranslation) {
                        item {
                            InfoBanner(
                                text = state.translationError ?: "",
                                icon = Icons.Outlined.ErrorOutline,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (state.translationOffline && state.translation != null && settings.showTranslation) {
                        item {
                            InfoBanner(
                                text = t(bn, "সংরক্ষিত অনুবাদ দেখানো হচ্ছে (অফলাইন)।", "Showing a downloaded translation (offline)."),
                                icon = Icons.Outlined.CloudDone
                            )
                        }
                    }

                    items(surah.ayahs, key = { it.numberInSurah }) { ayah ->
                        AyahRow(
                            ayah = ayah,
                            surahNumber = surah.number,
                            translation = if (settings.showTranslation) state.translation?.ayahs?.get(ayah.numberInSurah) else null,
                            translationEdition = settings.translationEdition,
                            bookmarked = state.bookmarks.contains(ayah.numberInSurah),
                            playing = audio.surah == surah.number && audio.ayah == ayah.numberInSurah && audio.playing,
                            bangla = bn,
                            onPlay = { viewModel.playAyah(surah, ayah.numberInSurah, settings) },
                            onBookmark = { viewModel.toggleBookmark(surah.number, ayah.numberInSurah) },
                            onVisible = { viewModel.onAyahVisible(surah.number, ayah.numberInSurah) }
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            QuranEditions.labelFor(settings.translationEdition) + " · " +
                                t(bn, "অনুবাদ মানে অর্থের অনুবাদ; মূল আরবি টেক্সট অপরিবর্তিত।",
                                    "A translation renders meaning; the Arabic text is unchanged."),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showDownloadDialog) {
        DownloadDialog(
            bangla = bn,
            busy = state.downloadProgress != null,
            progress = state.downloadProgress,
            message = state.downloadMessage,
            onDismiss = {
                showDownloadDialog = false
                viewModel.clearDownloadMessage()
            },
            onConfirm = { includeAudio -> viewModel.downloadSurah(surahNumber, settings, includeAudio) }
        )
    }
}

@Composable
private fun AyahRow(
    ayah: Ayah,
    surahNumber: Int,
    translation: String?,
    translationEdition: String,
    bookmarked: Boolean,
    playing: Boolean,
    bangla: Boolean,
    onPlay: () -> Unit,
    onBookmark: () -> Unit,
    onVisible: () -> Unit
) {
    LaunchedEffect(ayah.numberInSurah) { onVisible() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (playing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .semantics {
                contentDescription = "Surah $surahNumber ayah ${ayah.numberInSurah}" +
                    if (bookmarked) ", bookmarked" else ""
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    Text("${ayah.numberInSurah}", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.weight(1f))
            if (ayah.sajda) {
                Text(
                    t(bangla, "সিজদা", "Sajda"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(6.dp))
            }
            IconButton(onClick = onPlay, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (playing) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause recitation" else "Play recitation",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onBookmark, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (bookmarked) "Remove bookmark" else "Add bookmark",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            ayah.text,
            style = arabicStyle(sizeSp = 26f),
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        if (!translation.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                QuranEditions.labelFor(translationEdition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AudioBar(
    state: com.salahmate.app.data.quran.AudioState,
    bangla: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayFromCurrent: (Int) -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = t(bangla, "আগের আয়াত", "Previous ayah"))
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (state.playing) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playing) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = t(bangla, "পরের আয়াত", "Next ayah"))
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        t(bangla, "আয়াত", "Ayah") + " ${state.ayah}" +
                            if (state.usingLocalFile) t(bangla, " · অফলাইন", " · offline") else "",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (state.buffering) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                    } else {
                        Slider(
                            value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                            onValueChange = { fraction -> onSeek((fraction * state.durationMs).toLong()) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
            state.error?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DownloadDialog(
    bangla: Boolean,
    busy: Boolean,
    progress: Float?,
    message: String?,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(t(bangla, "অফলাইনের জন্য সংরক্ষণ", "Save for offline use")) },
        text = {
            Column {
                Text(
                    t(
                        bangla,
                        "এই সূরার অনুবাদ সংরক্ষণ করা হবে। চাইলে আরবি তেলাওয়াতের অডিওও ডাউনলোড করতে পারেন (প্রতি সূরার জন্য কয়েক মেগাবাইট)।",
                        "The translation for this surah will be stored on the device. You can also download the recitation audio (a few megabytes per surah)."
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (progress != null) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                }
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(true) }, enabled = !busy) {
                Text(t(bangla, "টেক্সট + অডিও", "Text + audio"))
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(false) }, enabled = !busy) {
                Text(t(bangla, "শুধু অনুবাদ", "Translation only"))
            }
        }
    )
}
