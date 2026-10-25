package com.salahmate.app.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salahmate.app.AppContainer
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.quran.AudioState
import com.salahmate.app.data.quran.QuranAudioController
import com.salahmate.app.data.quran.Surah
import com.salahmate.app.data.quran.SurahTranslation
import com.salahmate.app.data.quran.TranslationResult
import com.salahmate.app.data.quran.QuranEditions
import com.salahmate.app.data.prayer.ProviderError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SurahListUiState(
    val loading: Boolean = true,
    val surahs: List<Surah> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val lastReadSurah: Int? = null,
    val lastReadAyah: Int? = null,
)

data class ReaderUiState(
    val loading: Boolean = true,
    val surah: Surah? = null,
    val translation: SurahTranslation? = null,
    val translationLoading: Boolean = false,
    val translationError: String? = null,
    val translationOffline: Boolean = false,
    val bookmarks: Set<Int> = emptySet(),
    val error: String? = null,
    val dailyAyahs: Int = 0,
    val downloadProgress: Float? = null,
    val downloadMessage: String? = null,
)

/**
 * Quran reading state: offline Arabic text, on-demand translations (cached to disk), bookmarks,
 * last-read position and downloads.
 */
class QuranViewModel(private val container: AppContainer) : ViewModel() {

    private val audio = QuranAudioController(container.appContext, viewModelScope)
    val audioState: StateFlow<AudioState> = audio.state

    val settings: StateFlow<UserSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _list = MutableStateFlow(SurahListUiState())
    val list: StateFlow<SurahListUiState> = _list

    private val _reader = MutableStateFlow(ReaderUiState())
    val reader: StateFlow<ReaderUiState> = _reader

    init {
        viewModelScope.launch {
            val surahs = container.quran.surahIndex()
            // Register ayah counts so per-ayah audio URLs can be derived without extra requests.
            QuranAudioController.registerAyahCounts(surahs.map { it.ayahCount })
            val position = container.database.quranDao().readingPositionOnce()
            _list.value = SurahListUiState(
                loading = false,
                surahs = surahs,
                lastReadSurah = position?.surah,
                lastReadAyah = position?.ayah,
            )
        }
    }

    fun onSearch(query: String) {
        _list.value = _list.value.copy(query = query)
    }

    fun filteredSurahs(): List<Surah> {
        val q = _list.value.query.trim()
        if (q.isEmpty()) return _list.value.surahs
        val lower = q.lowercase()
        return _list.value.surahs.filter { surah ->
            surah.number.toString() == q ||
                surah.nameEnglish.lowercase().contains(lower) ||
                surah.nameTranslation.lowercase().contains(lower) ||
                surah.nameBengali.contains(q) ||
                surah.nameArabic.contains(q)
        }
    }

    fun openSurah(number: Int, startAyah: Int?, settings: UserSettings) {
        _reader.value = ReaderUiState(loading = true)
        viewModelScope.launch {
            when (val result = container.quran.surah(number)) {
                is com.salahmate.app.data.quran.SurahResult.Success -> {
                    val surah = result.surah
                    val bookmarks = container.database.quranDao().bookmarks().first()
                        .filter { it.surah == number }.map { it.ayah }.toSet()
                    val daily = container.database.quranDao().dailyReading(java.time.LocalDate.now().toString())?.ayahsRead ?: 0
                    _reader.value = ReaderUiState(
                        loading = false,
                        surah = surah,
                        bookmarks = bookmarks,
                        dailyAyahs = daily,
                    )
                    loadTranslation(number, settings)
                    audio.setReciter(settings.reciterEdition)
                    val start = startAyah ?: 1
                    container.quran.saveReadingPosition(number, start)
                    container.database.quranDao().updateHistory(number, start, System.currentTimeMillis())
                }
                is com.salahmate.app.data.quran.SurahResult.Failure -> {
                    _reader.value = ReaderUiState(loading = false, error = result.message)
                }
            }
        }
    }

    fun loadTranslation(surah: Int, settings: UserSettings, forceNetwork: Boolean = false) {
        val edition = settings.translationEdition
        _reader.value = _reader.value.copy(translationLoading = true, translationError = null)
        viewModelScope.launch {
            val cached = if (forceNetwork) null else container.quran.cachedTranslationEdition(surah, edition)
            if (cached != null) {
                _reader.value = _reader.value.copy(
                    translation = cached,
                    translationLoading = false,
                    translationOffline = true,
                )
                return@launch
            }
            when (val result = container.quran.translation(surah, edition)) {
                is TranslationResult.Success -> _reader.value = _reader.value.copy(
                    translation = result.translation,
                    translationLoading = false,
                    translationOffline = result.translation.fromCache,
                    translationError = null,
                )
                is TranslationResult.Failure -> _reader.value = _reader.value.copy(
                    translationLoading = false,
                    translationError = when (result.reason) {
                        ProviderError.NETWORK_UNAVAILABLE -> "No connection and this translation is not downloaded yet."
                        ProviderError.TIMEOUT -> "The translation request timed out."
                        ProviderError.RATE_LIMITED -> "Too many requests — try again in a moment."
                        else -> result.message ?: "Could not load the translation."
                    },
                )
            }
        }
    }

    fun onAyahVisible(surah: Int, ayah: Int) {
        viewModelScope.launch {
            container.quran.saveReadingPosition(surah, ayah)
            container.quran.addReadAyahs(1)
            val daily = container.database.quranDao().dailyReading(java.time.LocalDate.now().toString())?.ayahsRead ?: 0
            _reader.value = _reader.value.copy(dailyAyahs = daily)
        }
    }

    fun toggleBookmark(surah: Int, ayah: Int) {
        viewModelScope.launch {
            val existing = container.database.quranDao().bookmarkAt(surah, ayah)
            if (existing == null) {
                container.quran.addBookmark(surah, ayah)
                _reader.value = _reader.value.copy(bookmarks = _reader.value.bookmarks + ayah)
            } else {
                container.quran.removeBookmark(surah, ayah)
                _reader.value = _reader.value.copy(bookmarks = _reader.value.bookmarks - ayah)
            }
        }
    }

    // ----------------------------------------------------------------- audio --

    fun playAyah(surah: Surah, ayah: Int, settings: UserSettings) {
        audio.playSurah(surah, ayah, settings.reciterEdition)
    }

    fun togglePlayPause() = audio.togglePlayPause()
    fun nextAyah() = audio.next()
    fun previousAyah() = audio.previous()
    fun seekTo(positionMs: Long) = audio.seekTo(positionMs)
    fun stopAudio() = audio.stop()

    // ------------------------------------------------------------- downloads --

    /**
     * Downloads the selected translation for one surah (small JSON) and, optionally, the audio for
     * every ayah. Progress is reported; files are tracked so users can see and clear storage.
     */
    fun downloadSurah(surahNumber: Int, settings: UserSettings, includeAudio: Boolean) {
        _reader.value = _reader.value.copy(downloadProgress = 0f, downloadMessage = null)
        viewModelScope.launch {
            val surah = (_reader.value.surah ?: return@launch).takeIf { it.number == surahNumber } ?: return@launch
            // 1. Translation
            val translationResult = container.quran.translation(surahNumber, settings.translationEdition)
            if (translationResult is TranslationResult.Failure) {
                _reader.value = _reader.value.copy(
                    downloadProgress = null,
                    downloadMessage = "Translation download failed: ${translationResult.message ?: translationResult.reason.name}",
                )
                return@launch
            }
            if (includeAudio) {
                val total = surah.ayahCount
                var done = 0
                val reciter = settings.reciterEdition
                for (ayah in 1..total) {
                    val global = QuranAudioController.globalAyahNumber(surahNumber, ayah) ?: continue
                    val file = audio.localAudioFile(reciter, global)
                    if (!file.exists()) {
                        val ok = runCatching {
                            withContext(Dispatchers.IO) {
                                val url = java.net.URL(QuranEditions.ayahAudioUrl(reciter, global))
                                url.openStream().use { input ->
                                    file.parentFile?.mkdirs()
                                    file.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                            true
                        }.getOrDefault(false)
                        if (!ok) {
                            _reader.value = _reader.value.copy(
                                downloadProgress = null,
                                downloadMessage = "Audio download stopped at ayah $ayah. Check your connection.",
                            )
                            return@launch
                        }
                    }
                    done++
                    _reader.value = _reader.value.copy(downloadProgress = done.toFloat() / total)
                }
            }
            _reader.value = _reader.value.copy(
                downloadProgress = null,
                downloadMessage = if (includeAudio) "Surah downloaded for offline use (text + audio)." else "Translation downloaded for offline use.",
                translationOffline = true,
            )
        }
    }

    fun clearDownloadMessage() {
        _reader.value = _reader.value.copy(downloadMessage = null)
    }

    fun storageBytes(): Long = File(container.appContext.filesDir, "quran").walkTopDown().filter { it.isFile }.sumOf { it.length() }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}
