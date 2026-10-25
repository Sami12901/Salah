package com.salahmate.app

/** Type-safe-ish route catalogue. Kept in one place so deep links and the bottom bar agree. */
object Routes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val QURAN_READER = "quran/reader/{surah}"
    const val TRACK = "track"
    const val LEARN = "learn"
    const val MORE = "more"
    const val TIMETABLE = "timetable"

    const val HIFZ = "hifz"
    const val HIFZ_DETAIL = "hifz/detail/{id}"
    const val PRAYER_TRACKING = "tracking"
    const val TASBIH = "tasbih"
    const val HADITH = "hadith"
    const val HADITH_BOOK = "hadith/{collection}"
    const val DUA = "dua"
    const val DUA_CATEGORY = "dua/{category}"
    const val LEARNING = "learning"
    const val ASMA = "learning/asma"
    const val QIBLA = "qibla"
    const val ZAKAT = "zakat"
    const val RAMADAN = "ramadan"
    const val CALENDAR = "calendar"
    const val MOSQUE = "mosque"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRAYER_SETTINGS = "settings/prayer"
    const val NOTIFICATION_SETTINGS = "settings/notifications"
    const val QURAN_SETTINGS = "settings/quran"
    const val DATA_SETTINGS = "settings/data"

    fun reader(surah: Int, ayah: Int? = null): String =
        if (ayah == null) "quran/reader/$surah" else "quran/reader/$surah?ayah=$ayah"

    fun hadithBook(collection: String): String = "hadith/$collection"
    fun duaCategory(category: String): String = "dua/$category"
}
