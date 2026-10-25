package com.salahmate.app.core.util

import java.time.LocalDate

/**
 * Hijri (Umm al-Qura style arithmetic) date conversion.
 *
 * This is the widely used *tabular/Kuwaiti* algorithm (leap years 2, 5, 7, 10, 13, 16, 18, 21,
 * 24, 26, 29 within a 30-year cycle). It is deterministic, needs no network, and is accurate to
 * within one day of observed calendars. Local moon-sighting committees may announce a date one
 * day earlier or later, which is why the app exposes a manual ±N day offset and never claims to
 * be an official authority.
 */
data class HijriDate(
    val year: Int,
    val month: Int,     // 1..12
    val day: Int,       // 1..30
    val monthNameEn: String,
    val monthNameAr: String,
    val monthNameBn: String,
    val adjustmentDays: Int = 0,
) {
    val formattedEn: String get() = "$day $monthNameEn $year AH"
    val formattedBn: String get() = "$day $monthNameBn $year হিজরি"
    val formattedAr: String get() = "$day $monthNameAr $year هـ"
    /** ISO-like machine form, useful for storage and tests. */
    val iso: String get() = "%04d-%02d-%02d".format(year, month, day)
}

object HijriCalendar {

    private val MONTHS_EN = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula", "Jumada al-Akhirah",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah",
    )
    private val MONTHS_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )
    private val MONTHS_BN = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি", "জুমাদাল উলা", "জুমাদাস সানিয়া",
        "রজব", "শাবান", "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ",
    )

    /** Gregorian date -> Julian Day Number (integer, Gregorian calendar). */
    fun gregorianToJdn(date: LocalDate): Long {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth
        val a = (14 - m) / 12
        val y2 = y + 4800 - a
        val m2 = m + 12 * a - 3
        return d + (153L * m2 + 2) / 5 + 365L * y2 + y2 / 4 - y2 / 100 + y2 / 400 - 32045
    }

    /** Julian Day Number -> Hijri (tabular) triple. */
    fun jdnToHijri(jdn: Long): Triple<Int, Int, Int> {
        // Epoch: 1 Muharram 1 AH = JD 1948440 (civil, Friday epoch).
        val l0 = jdn - 1948440 + 10632
        val n = (l0 - 1) / 10631
        val l = l0 - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        val l1 = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val month = (24 * l1 / 709).toInt()
        val day = (l1 - (709 * month / 24)).toInt()
        val year = (30 * n + j - 30).toInt()
        return Triple(year, month, day)
    }

    fun fromGregorian(date: LocalDate, adjustmentDays: Int = 0): HijriDate {
        val jdn = gregorianToJdn(date) + adjustmentDays
        val (y, m, d) = jdnToHijri(jdn)
        val idx = (m - 1).coerceIn(0, 11)
        return HijriDate(
            year = y,
            month = m,
            day = d,
            monthNameEn = MONTHS_EN[idx],
            monthNameAr = MONTHS_AR[idx],
            monthNameBn = MONTHS_BN[idx],
            adjustmentDays = adjustmentDays,
        )
    }

    /** Hijri (tabular) -> Julian Day Number. */
    fun hijriToJdn(hijriYear: Int, hijriMonth: Int, hijriDay: Int): Long {
        val monthDays = Math.ceil(29.5 * (hijriMonth - 1)).toLong()
        return hijriDay + monthDays + (hijriYear - 1) * 354L + (3 + 11L * hijriYear) / 30 + 1948440 - 1
    }

    /** Hijri (tabular) -> Gregorian date. */
    fun gregorianFor(hijriYear: Int, hijriMonth: Int, hijriDay: Int): LocalDate =
        jdnToGregorian(hijriToJdn(hijriYear, hijriMonth, hijriDay))
            ?: error("invalid hijri date $hijriYear-$hijriMonth-$hijriDay")

    private fun jdnToGregorian(jdn: Long): LocalDate? = try {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - 146097 * b / 4
        val d = (4 * c + 3) / 1461
        val e = c - 1461 * d / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        val year = (100 * b + d - 4800 + m / 10).toInt()
        LocalDate.of(year, month, day)
    } catch (t: Throwable) {
        null
    }

    /** Localised Hijri month name (1 = Muharram). */
    fun monthName(hijriMonth: Int, bangla: Boolean): String =
        (if (bangla) MONTHS_BN else MONTHS_EN)[(hijriMonth - 1).coerceIn(0, 11)]

    fun monthLength(hijriYear: Int, hijriMonth: Int): Int {
        val first = gregorianToJdn(gregorianFor(hijriYear, hijriMonth, 1))
        val nextY = if (hijriMonth == 12) hijriYear + 1 else hijriYear
        val nextM = if (hijriMonth == 12) 1 else hijriMonth + 1
        val next = gregorianToJdn(gregorianFor(nextY, nextM, 1))
        return (next - first).toInt().coerceIn(29, 30)
    }
}
