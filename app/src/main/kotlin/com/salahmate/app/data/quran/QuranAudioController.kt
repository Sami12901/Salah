package com.salahmate.app.data.quran

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Playback state the reader UI binds to. */
data class AudioState(
    val playing: Boolean = false,
    val surah: Int = 0,
    val ayah: Int = 0,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val buffering: Boolean = false,
    val error: String? = null,
    val usingLocalFile: Boolean = false,
)

/**
 * Ayah-by-ayah recitation using Media3's ExoPlayer.
 *
 * Streams from the AlQuran.cloud CDN (https://cdn.islamic.network/quran/audio/...) and prefers a
 * downloaded local file when one exists, so downloaded surahs play with no network.
 * Auto-advance gives continuous surah playback without a separate playlist implementation.
 */
class QuranAudioController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var player: ExoPlayer? = null
    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state

    private var currentSurah: Surah? = null
    private var reciterId: String = "ar.alafasy"

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val exo = ExoPlayer.Builder(context).build()
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _state.value = _state.value.copy(buffering = true)
                    Player.STATE_READY -> _state.value = _state.value.copy(
                        buffering = false,
                        durationMs = exo.duration.coerceAtLeast(0),
                    )
                    Player.STATE_ENDED -> advance()
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(playing = isPlaying)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.value = _state.value.copy(
                    playing = false,
                    buffering = false,
                    error = "Playback failed: ${error.errorCodeName}",
                )
            }
        })
        player = exo
        // Position ticker for the seek bar.
        scope.launch {
            while (true) {
                player?.let { p ->
                    if (p.isPlaying) {
                        _state.value = _state.value.copy(
                            positionMs = p.currentPosition,
                            durationMs = p.duration.coerceAtLeast(0),
                        )
                    }
                }
                delay(400)
            }
        }
        return exo
    }

    fun setReciter(reciterId: String) {
        if (this.reciterId != reciterId) {
            this.reciterId = reciterId
            _state.value = _state.value.copy(error = null)
        }
    }

    fun playSurah(surah: Surah, startAyah: Int, reciterId: String) {
        this.currentSurah = surah
        this.reciterId = reciterId
        playAyah(surah, startAyah)
    }

    fun playAyah(surah: Surah, ayah: Int) {
        currentSurah = surah
        val item = ayahMediaItem(surah, ayah) ?: return
        val exo = ensurePlayer()
        exo.setMediaItem(item)
        exo.prepare()
        exo.play()
        _state.value = _state.value.copy(
            surah = surah.number,
            ayah = ayah,
            playing = true,
            error = null,
            usingLocalFile = AyahAudio.sourceFor(localExists = item.localConfiguration?.uri?.scheme == "file") == AudioSource.LOCAL_FILE,
        )
    }

    fun togglePlayPause() {
        val exo = player ?: return
        if (exo.isPlaying) exo.pause() else exo.play()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0))
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun next() = advance()

    fun previous() {
        val surah = currentSurah ?: return
        playAyah(surah, AyahAudio.previousAyah(_state.value.ayah))
    }

    private fun advance() {
        val surah = currentSurah ?: return
        val next = AyahAudio.nextAyah(_state.value.ayah, surah.ayahCount)
        if (next == null) {
            _state.value = _state.value.copy(playing = false)
            return
        }
        playAyah(surah, next)
    }

    fun stop() {
        player?.stop()
        _state.value = AudioState()
    }

    fun release() {
        player?.release()
        player = null
        _state.value = AudioState()
    }

    /** Prefers a downloaded mp3 when present, otherwise streams from the CDN. */
    private fun ayahMediaItem(surah: Surah, ayah: Int): MediaItem? {
        val globalNumber = globalAyahNumber(surah.number, ayah) ?: return null
        val localFile = localAudioFile(reciterId, globalNumber)
        return if (localFile.exists()) {
            MediaItem.fromUri(android.net.Uri.fromFile(localFile))
        } else {
            MediaItem.fromUri(AyahAudio.url(reciterId, globalNumber))
        }
    }

    fun localAudioFile(reciterId: String, globalAyahNumber: Int): File =
        AyahAudio.localFile(context.filesDir, reciterId, globalAyahNumber)

    companion object {
        /** Offsets are derived from the bundled surah lengths, so no network or lookup table is needed. */
        fun globalAyahNumber(surah: Int, ayah: Int, ayahCounts: List<Int>): Int? {
            if (surah !in 1..ayahCounts.size) return null
            if (ayah !in 1..ayahCounts[surah - 1]) return null
            return ayahCounts.take(surah - 1).sum() + ayah
        }

        private var counts: List<Int>? = null

        /** Cached surah lengths; set once the repository has loaded the Quran text. */
        fun registerAyahCounts(list: List<Int>) {
            counts = list
        }

        fun globalAyahNumber(surah: Int, ayah: Int): Int? =
            counts?.let { globalAyahNumber(surah, ayah, it) }
    }
}
