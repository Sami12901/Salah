package com.salahmate.app.core.prefs

/** UI language. Arabic religious content is always shown in Arabic regardless of this value. */
enum class AppLanguage(val code: String) {
    BENGALI("bn"),
    ENGLISH("en"),
    ARABIC("ar");
    companion object {
        fun from(code: String?) = entries.firstOrNull { it.code == code } ?: BENGALI
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LocationMode { MANUAL, GPS }

/**
 * Prayer calculation methods. [adhan] is the Adhan library enum name, [aladhanId] is the
 * AlAdhan API `method` parameter. Mapping verified against
 * https://aladhan.com/prayer-times-api (method list) and adhan-java 1.2.1.
 *
 * UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI is AlAdhan `method=1` — the prompt's example uses
 * method=1 and this app keeps that identity explicit rather than guessing.
 */
enum class CalculationMethodId(
    val adhan: String,
    val aladhanId: Int,
    val labelEn: String,
    val labelBn: String,
) {
    MUSLIM_WORLD_LEAGUE("MUSLIM_WORLD_LEAGUE", 3, "Muslim World League", "মুসলিম ওয়ার্ল্ড লীগ"),
    KARACHI("KARACHI", 1, "University of Islamic Sciences, Karachi", "ইসলামিক সায়েন্সেস বিশ্ববিদ্যালয়, করাচি"),
    EGYPTIAN("EGYPTIAN", 5, "Egyptian General Authority of Survey", "মিসরীয় সাধারণ জরিপ কর্তৃপক্ষ"),
    UMM_AL_QURA("UMM_AL_QURA", 4, "Umm al-Qura University, Makkah", "উম্মুল কুরা বিশ্ববিদ্যালয়, মক্কা"),
    DUBAI("DUBAI", 16, "Dubai", "দুবাই"),
    QATAR("QATAR", 10, "Qatar", "কাতার"),
    KUWAIT("KUWAIT", 9, "Kuwait", "কুয়েত"),
    SINGAPORE("SINGAPORE", 11, "Singapore (MUIS)", "সিঙ্গাপুর"),
    MOON_SIGHTING_COMMITTEE("MOON_SIGHTING_COMMITTEE", 8, "Gulf Region", "উপসাগরীয় অঞ্চল"),
    NORTH_AMERICA("NORTH_AMERICA", 2, "ISNA (North America)", "ইসনা (উত্তর আমেরিকা)"),
    ;

    companion object {
        fun from(name: String?): CalculationMethodId =
            entries.firstOrNull { it.name == name } ?: KARACHI
    }
}

/** Asr shadow-length school, kept strictly separate from the calculation method. */
enum class AsrMadhab(val adhan: String, val aladhanSchool: Int, val labelEn: String, val labelBn: String) {
    SHAFI("SHAFI", 0, "Shafi'i / Maliki / Hanbali (standard)", "শাফেয়ী / মালিকী / হাম্বলী (প্রচলিত)"),
    HANAFI("HANAFI", 1, "Hanafi (later shadow)", "হানাফী (দ্বিগুণ ছায়া)"),
    ;

    companion object {
        fun from(name: String?): AsrMadhab = entries.firstOrNull { it.name == name } ?: HANAFI
    }
}

enum class HighLatitudeRuleId { MIDDLE_OF_THE_NIGHT, SEVENTH_OF_THE_NIGHT, TWILIGHT_ANGLE }

/** Where prayer times come from. */
enum class PrayerProviderMode {
    /** Adhan library on device. Works offline. Default. */
    LOCAL,

    /** AlAdhan API only; shows an explicit error state when offline. */
    ALADHAN_ONLINE,

    /** AlAdhan API when reachable, Adhan library fallback with a visible label. */
    ALADHAN_WITH_LOCAL_FALLBACK,
}

enum class NotificationSoundMode { SILENT, SYSTEM_SOUND, ADHAN_AUDIO }

enum class NisabBasis { GOLD, SILVER }

/** Immutable snapshot of every user preference the app reads. */
data class UserSettings(
    val language: AppLanguage = AppLanguage.BENGALI,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false,

    // Location
    val locationMode: LocationMode = LocationMode.MANUAL,
    val latitude: Double = 23.8103,
    val longitude: Double = 90.4125,
    val locationLabel: String = "Dhaka, Bangladesh",
    val timeZoneId: String = "Asia/Dhaka",
    /** Used for city-based AlAdhan queries; null when the location came from GPS. */
    val city: String? = "Dhaka",
    val country: String? = "Bangladesh",

    // Calculation
    val method: CalculationMethodId = CalculationMethodId.KARACHI,
    val madhab: AsrMadhab = AsrMadhab.HANAFI,
    val highLatitudeRule: HighLatitudeRuleId = HighLatitudeRuleId.MIDDLE_OF_THE_NIGHT,
    val adjustmentFajr: Int = 0,
    val adjustmentSunrise: Int = 0,
    val adjustmentDhuhr: Int = 0,
    val adjustmentAsr: Int = 0,
    val adjustmentMaghrib: Int = 0,
    val adjustmentIsha: Int = 0,
    val providerMode: PrayerProviderMode = PrayerProviderMode.LOCAL,

    // Quran
    val translationEdition: String = "bn.bengali",
    val showTranslation: Boolean = true,
    val reciterEdition: String = "ar.alafasy",
    val arabicScale: Float = 1.0f,
    val dailyAyahGoal: Int = 10,

    // Notifications
    val notificationsEnabled: Boolean = false,
    val notifyFajr: Boolean = true,
    val notifyDhuhr: Boolean = true,
    val notifyAsr: Boolean = true,
    val notifyMaghrib: Boolean = true,
    val notifyIsha: Boolean = true,
    val soundMode: NotificationSoundMode = NotificationSoundMode.SYSTEM_SOUND,
    val advanceMinutes: Int = 0,
    val vibrate: Boolean = true,

    // Ramadan
    val suhoorReminderEnabled: Boolean = false,
    val iftarReminderEnabled: Boolean = false,
    val imsakOffsetMinutes: Int = 10,
    val iftarOffsetMinutes: Int = 0,

    // Calendar
    val hijriOffsetDays: Int = 0,

    // Hifz & tracking
    val dailyRevisionGoal: Int = 5,
    val prayerTrackingEnabled: Boolean = false,

    // Zakat
    val nisabBasis: NisabBasis = NisabBasis.GOLD,
    val goldPricePerGram: Double = 0.0,
    val silverPricePerGram: Double = 0.0,
    val zakatCurrency: String = "BDT",
    val goldPriceUpdatedAt: Long = 0L,

    val lastExportAt: Long = 0L,
) {
    val isBanglaUi: Boolean get() = language == AppLanguage.BENGALI
    val isArabicUi: Boolean get() = language == AppLanguage.ARABIC
    val isRtl: Boolean get() = isArabicUi
    fun adjustmentFor(prayer: String): Int = when (prayer) {
        "FAJR" -> adjustmentFajr
        "SUNRISE" -> adjustmentSunrise
        "DHUHR" -> adjustmentDhuhr
        "ASR" -> adjustmentAsr
        "MAGHRIB" -> adjustmentMaghrib
        "ISHA" -> adjustmentIsha
        else -> 0
    }
    fun isPrayerNotificationEnabled(prayer: String): Boolean = when (prayer) {
        "FAJR" -> notifyFajr
        "DHUHR" -> notifyDhuhr
        "ASR" -> notifyAsr
        "MAGHRIB" -> notifyMaghrib
        "ISHA" -> notifyIsha
        else -> false
    }
}
