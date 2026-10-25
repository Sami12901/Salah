package com.salahmate.app.feature.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salahmate.app.AppContainer
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.hadith.Hadith
import com.salahmate.app.data.hadith.HadithCollection
import com.salahmate.app.data.hadith.HadithManifest
import com.salahmate.app.data.hadith.HadithMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HadithUiState(
    val loading: Boolean = true,
    val manifest: HadithManifest = HadithManifest(),
    val downloadedIds: Set<String> = emptySet(),
    val bookmarkedNumbers: Set<Int> = emptySet(),
    val busyCollection: String? = null,
    val message: String? = null,
)

/**
 * Hadith library.
 *
 * Only collections that are actually available on the device are readable: the two bundled
 * collections (Forty Hadith of an-Nawawi and Forty Hadith Qudsi) plus any collection the user
 * explicitly downloaded. Nothing is generated or scraped at runtime, and every narration shows its
 * reference; grading is displayed exactly as supplied by the source, with its scholar attributed.
 */
class HadithViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(HadithUiState())
    val ui: StateFlow<HadithUiState> = _ui

    private val _openCollection = MutableStateFlow<HadithCollection?>(null)
    val openCollection: StateFlow<HadithCollection?> = _openCollection

    init {
        refresh()
        viewModelScope.launch {
            container.database.hadithDao().bookmarks().collect { marks ->
                _ui.value = _ui.value.copy(bookmarkedNumbers = marks.map { it.hadithNumber }.toSet())
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val manifest = container.hadith.manifest()
            val downloaded = manifest.downloadable.filter { container.hadith.isDownloaded(it.id) }.map { it.id }.toSet()
            _ui.value = _ui.value.copy(
                loading = false,
                manifest = manifest,
                downloadedIds = downloaded,
            )
        }
    }

    fun open(meta: HadithMeta) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val collection = container.hadith.bundledCollection(meta.id)
                ?: container.hadith.downloadedCollection(meta.id)
            _openCollection.value = collection
            _ui.value = _ui.value.copy(
                loading = false,
                message = if (collection == null) "This collection is not on the device yet." else null,
            )
        }
    }

    fun close() {
        _openCollection.value = null
    }

    fun download(meta: HadithMeta) {
        _ui.value = _ui.value.copy(busyCollection = meta.id, message = null)
        viewModelScope.launch {
            val result = container.hadith.downloadCollection(meta.id)
            _ui.value = if (result.isSuccess) {
                _ui.value.copy(
                    busyCollection = null,
                    downloadedIds = _ui.value.downloadedIds + meta.id,
                    message = "${meta.name} downloaded — available offline.",
                )
            } else {
                _ui.value.copy(
                    busyCollection = null,
                    message = "Download failed: ${result.exceptionOrNull()?.message ?: "unknown error"}",
                )
            }
        }
    }

    fun delete(meta: HadithMeta) {
        viewModelScope.launch {
            container.hadith.deleteDownloaded(meta.id)
            _ui.value = _ui.value.copy(
                downloadedIds = _ui.value.downloadedIds - meta.id,
                message = "${meta.name} removed from this device.",
            )
        }
    }

    fun toggleBookmark(hadith: Hadith, collectionId: String) {
        viewModelScope.launch {
            if (_ui.value.bookmarkedNumbers.contains(hadith.number)) {
                container.database.hadithDao().unbookmark(collectionId, hadith.number)
            } else {
                container.database.hadithDao().bookmark(
                    com.salahmate.app.core.db.HadithBookmarkEntity(
                        collection = collectionId,
                        bookNumber = hadith.book,
                        hadithNumber = hadith.number,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    /** Language preference for narration text; falls back when a translation is missing. */
    fun body(hadith: Hadith, settings: UserSettings): String =
        if (settings.isBanglaUi) hadith.bengali.ifBlank { hadith.english } else hadith.english.ifBlank { hadith.bengali }
}
