package com.salahmate.app.data

import com.salahmate.app.data.quran.AudioSource
import com.salahmate.app.data.quran.AyahAudio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Ayah audio addressing and stepping for the Quran reader. */
class AyahAudioTest {

    @Test
    fun `stream url matches the verified cdn layout`() {
        assertEquals(
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3",
            AyahAudio.url("ar.alafasy", 262),
        )
        assertEquals(
            "https://cdn.islamic.network/quran/audio/64/ar.husary/1.mp3",
            AyahAudio.url("ar.husary", 1, bitrate = 64),
        )
    }

    @Test
    fun `local cache path is app private and per reciter`() {
        val files = File("/data/user/0/com.salahmate.app/files")
        val expected = File(
            "/data/user/0/com.salahmate.app/files/quran/audio/ar.alafasy/262.mp3",
        )
        assertEquals(expected.path, AyahAudio.localFile(files, "ar.alafasy", 262).path)
        // Different reciters must not share a path, otherwise one reciter overwrites another.
        assertTrue(
            AyahAudio.localFile(files, "ar.husary", 262).path !=
                AyahAudio.localFile(files, "ar.alafasy", 262).path,
        )
        assertEquals("262.mp3", AyahAudio.fileName(262))
    }

    @Test
    fun `next ayah stops at the end of the surah`() {
        assertEquals(2, AyahAudio.nextAyah(1, 7))
        assertEquals(7, AyahAudio.nextAyah(6, 7))
        assertNull(AyahAudio.nextAyah(7, 7))
        assertNull(AyahAudio.nextAyah(8, 7))
        // Single-ayah surahs (al-Kawthar is 3, but a 1-ayah edge case must not loop).
        assertNull(AyahAudio.nextAyah(1, 1))
    }

    @Test
    fun `previous ayah clamps at the first ayah`() {
        assertEquals(1, AyahAudio.previousAyah(1))
        assertEquals(1, AyahAudio.previousAyah(2))
        assertEquals(10, AyahAudio.previousAyah(11))
        assertEquals(1, AyahAudio.previousAyah(0))
    }

    @Test
    fun `source reports local playback only when the file exists`() {
        assertEquals(AudioSource.LOCAL_FILE, AyahAudio.sourceFor(localExists = true))
        assertEquals(AudioSource.STREAM, AyahAudio.sourceFor(localExists = false))
    }
}
