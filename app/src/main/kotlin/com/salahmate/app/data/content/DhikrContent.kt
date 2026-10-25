package com.salahmate.app.data.content

/** Tasbih phrases. Targets are the counts transmitted in the cited narrations where stated. */
data class DhikrPhrase(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val bengali: String,
    val english: String,
    val defaultTarget: Int,
    val source: String,
)

object DhikrContent {
    val phrases = listOf(
        DhikrPhrase(
            id = "subhanallah",
            arabic = "سُبْحَانَ اللَّهِ",
            transliteration = "SubhanAllah",
            bengali = "আল্লাহ পবিত্র।",
            english = "Glory be to Allah.",
            defaultTarget = 33,
            source = "Sahih Muslim 597 (tasbih 33x after prayer)",
        ),
        DhikrPhrase(
            id = "alhamdulillah",
            arabic = "الْحَمْدُ لِلَّهِ",
            transliteration = "Alhamdulillah",
            bengali = "সকল প্রশংসা আল্লাহর।",
            english = "All praise is for Allah.",
            defaultTarget = 33,
            source = "Sahih Muslim 597",
        ),
        DhikrPhrase(
            id = "allahuakbar",
            arabic = "اللَّهُ أَكْبَرُ",
            transliteration = "Allahu Akbar",
            bengali = "আল্লাহ সর্বশ্রেষ্ঠ।",
            english = "Allah is the Greatest.",
            defaultTarget = 34,
            source = "Sahih Muslim 597",
        ),
        DhikrPhrase(
            id = "tahlil",
            arabic = "لَا إِلَهَ إِلَّا اللَّهُ",
            transliteration = "La ilaha illallah",
            bengali = "আল্লাহ ছাড়া কোনো উপাস্য নেই।",
            english = "There is no god but Allah.",
            defaultTarget = 100,
            source = "Sahih al-Bukhari 3293; Sahih Muslim 2691",
        ),
        DhikrPhrase(
            id = "istighfar",
            arabic = "أَسْتَغْفِرُ اللَّهَ",
            transliteration = "Astaghfirullah",
            bengali = "আমি আল্লাহর কাছে ক্ষমা চাই।",
            english = "I seek Allah's forgiveness.",
            defaultTarget = 100,
            source = "Sahih Muslim 2702 (seeking forgiveness abundantly)",
        ),
        DhikrPhrase(
            id = "subhanallah_wa_bihamdih",
            arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            transliteration = "SubhanAllahi wa bihamdih",
            bengali = "আল্লাহ পবিত্র ও সকল প্রশংসা তাঁরই।",
            english = "Glory be to Allah and praise be to Him.",
            defaultTarget = 100,
            source = "Sahih Muslim 2692",
        ),
        DhikrPhrase(
            id = "hawqala",
            arabic = "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            transliteration = "La hawla wa la quwwata illa billah",
            bengali = "আল্লাহর সাহায্য ছাড়া কোনো শক্তি বা সামর্থ্য নেই।",
            english = "There is no power and no strength except with Allah.",
            defaultTarget = 100,
            source = "Sahih al-Bukhari 6384",
        ),
        DhikrPhrase(
            id = "salawat",
            arabic = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ",
            transliteration = "Allahumma salli 'ala Muhammadin wa 'ala ali Muhammad",
            bengali = "হে আল্লাহ, মুহাম্মদ ও তাঁর পরিবারের ওপর রহমত বর্ষণ করুন।",
            english = "O Allah, send blessings upon Muhammad and upon the family of Muhammad.",
            defaultTarget = 100,
            source = "Qur'an 33:56; Sahih al-Bukhari 3370 (instruction to send salawat)",
        ),
    )

    fun byId(id: String): DhikrPhrase? = phrases.firstOrNull { it.id == id } ?: phrases.firstOrNull()
}
