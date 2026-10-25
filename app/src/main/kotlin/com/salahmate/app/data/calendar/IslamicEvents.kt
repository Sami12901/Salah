package com.salahmate.app.data.calendar

/**
 * Islamic dates and observances.
 *
 * IMPORTANT: these are *calendar dates* in the Hijri year, not confirmed observances. Actual
 * dates depend on local moon sighting, so the app always presents them with a moon-sighting note
 * and never states that a future date is confirmed. `certainty` records how the date is treated:
 *  - SCRIPTURAL: the date follows directly from the Hijri month (e.g. 1 Ramadan begins the month).
 *  - OBSERVED: widely observed on this date but not fixed by the text (e.g. 27 Rajab).
 */
enum class EventCertainty { SCRIPTURAL, OBSERVED }

data class IslamicEvent(
    val id: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val titleEn: String,
    val titleBn: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val certainty: EventCertainty,
)

object IslamicEvents {

    val all = listOf(
        IslamicEvent(
            "new_year", 1, 1,
            "Islamic New Year", "হিজরি নতুন বছর",
            "The first day of Muharram, the start of the Hijri year.",
            "মুহাররমের প্রথম দিন, হিজরি বছরের সূচনা।",
            EventCertainty.SCRIPTURAL,
        ),
        IslamicEvent(
            "ashura", 1, 10,
            "Ashura", "আশুরা",
            "The tenth of Muharram. Fasting on this day is recommended in the Sunnah (alongside the ninth).",
            "মুহাররমের দশম দিন। এই দিনে (নবম দিনের সাথে) রোজা রাখা সুন্নাহে উৎসাহিত।",
            EventCertainty.SCRIPTURAL,
        ),
        IslamicEvent(
            "isra_miraj", 7, 27,
            "Isra' and Mi'raj", "ইসরা ও মিরাজ",
            "Commonly observed on 27 Rajab; the exact date is not established by the text.",
            "সাধারণত ২৭ রজবে পালিত হয়; সুনির্দিষ্ট তারিখ কুরআন-সুন্নাহ দ্বারা নির্ধারিত নয়।",
            EventCertainty.OBSERVED,
        ),
        IslamicEvent(
            "mid_shaban", 8, 15,
            "Mid-Sha'ban", "শবে বরাত",
            "The fifteenth of Sha'ban, observed by many communities.",
            "শাবানের পনেরো তারিখ, অনেক সমাজে পালিত।",
            EventCertainty.OBSERVED,
        ),
        IslamicEvent(
            "ramadan", 9, 1,
            "Ramadan begins", "রমজান শুরু",
            "The first day of Ramadan, the month of fasting. Its start depends on local moon sighting.",
            "রমজানের প্রথম দিন, রোজার মাস। শুরু স্থানীয় চাঁদ দেখার ওপর নির্ভরশীল।",
            EventCertainty.SCRIPTURAL,
        ),
        IslamicEvent(
            "laylatul_qadr", 9, 27,
            "Laylat al-Qadr (commonly sought)", "লাইলাতুল কদর (সাধারণত অন্বেষিত)",
            "The Night of Decree is sought in the odd nights of the last ten days of Ramadan; 27 Ramadan is the most commonly observed night.",
            "কদরের রাত রমজানের শেষ দশকের বিজোড় রাতে অন্বেষণ করা হয়; ২৭ রমজান সর্বাধিক প্রচলিত।",
            EventCertainty.OBSERVED,
        ),
        IslamicEvent(
            "eid_fitr", 10, 1,
            "Eid al-Fitr", "ঈদুল ফিতর",
            "The first of Shawwal, marking the end of Ramadan. The date depends on local moon sighting.",
            "শাওয়ালের প্রথম দিন, রমজানের সমাপ্তি। তারিখ স্থানীয় চাঁদ দেখার ওপর নির্ভরশীল।",
            EventCertainty.SCRIPTURAL,
        ),
        IslamicEvent(
            "arafah", 12, 9,
            "Day of Arafah", "আরাফার দিন",
            "The ninth of Dhu al-Hijjah, the day of standing at Arafah during Hajj.",
            "জিলহজের নবম দিন, হজে আরাফায় অবস্থানের দিন।",
            EventCertainty.SCRIPTURAL,
        ),
        IslamicEvent(
            "eid_adha", 12, 10,
            "Eid al-Adha", "ঈদুল আজহা",
            "The tenth of Dhu al-Hijjah, the festival of sacrifice.",
            "জিলহজের দশম দিন, কুরবানির ঈদ।",
            EventCertainty.SCRIPTURAL,
        ),
    )

    fun eventsInMonth(hijriMonth: Int): List<IslamicEvent> = all.filter { it.hijriMonth == hijriMonth }

    /** Events in the next [days] days of the Gregorian calendar, using the arithmetic Hijri date. */
    fun upcoming(
        from: java.time.LocalDate,
        days: Int,
        hijriOffset: Int,
        hijriFor: (java.time.LocalDate, Int) -> com.salahmate.app.core.util.HijriDate,
    ): List<Pair<IslamicEvent, java.time.LocalDate>> {
        val results = mutableListOf<Pair<IslamicEvent, java.time.LocalDate>>()
        for (i in 0..days) {
            val date = from.plusDays(i.toLong())
            val hijri = hijriFor(date, hijriOffset)
            all.filter { it.hijriMonth == hijri.month && it.hijriDay == hijri.day }
                .forEach { results.add(it to date) }
        }
        return results
    }

    val moonSightingNote: String =
        "Dates follow the Hijri calendar used in this app and may differ by a day from local " +
            "moon-sighting announcements. This app does not issue religious rulings or confirm sightings."
}
