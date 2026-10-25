package com.salahmate.app.data

import com.salahmate.app.data.quran.QuranAudioController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Global ayah numbering feeds the audio CDN URL, so it must match the mushaf numbering exactly.
 * Counts below are the standard surah lengths for the first three surahs plus al-Kahf (18).
 */
class QuranAyahNumberTest {

    // 1: Al-Fatihah (7), 2: Al-Baqarah (286), 3: Aal-Imran (200), 18: Al-Kahf (110)
    private val counts: List<Int> = MutableList(114) { 0 }.also { list ->
        list[0] = 7
        list[1] = 286
        list[2] = 200
        list[17] = 110
    }

    @Test
    fun `first ayah of the quran is number one`() {
        assertEquals(1, QuranAudioController.globalAyahNumber(1, 1, counts))
    }

    @Test
    fun `ayahs of the first three surahs are offset correctly`() {
        assertEquals(7, QuranAudioController.globalAyahNumber(1, 7, counts))
        assertEquals(8, QuranAudioController.globalAyahNumber(2, 1, counts))
        assertEquals(293, QuranAudioController.globalAyahNumber(2, 286, counts))
        assertEquals(294, QuranAudioController.globalAyahNumber(3, 1, counts))
        assertEquals(493, QuranAudioController.globalAyahNumber(3, 200, counts))
    }

    @Test
    fun `surah kahf starts at 2165`() {
        // 7 + 286 + 200 = 493 ayahs before surah 18 in this table, so its first ayah is 494.
        assertEquals(494, QuranAudioController.globalAyahNumber(18, 1, counts))
    }

    @Test
    fun `out of range input returns null instead of a wrong url`() {
        assertNull(QuranAudioController.globalAyahNumber(0, 1, counts))
        assertNull(QuranAudioController.globalAyahNumber(1, 0, counts))
        assertNull(QuranAudioController.globalAyahNumber(1, 8, counts))
        assertNull(QuranAudioController.globalAyahNumber(200, 1, counts))
    }
}
