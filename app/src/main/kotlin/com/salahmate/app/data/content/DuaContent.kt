package com.salahmate.app.data.content

/**
 * Dua and daily Adhkar — curated local dataset.
 *
 * SOURCING RULES USED HERE (see CONTENT_LICENSES.md and the verification checklist):
 *  • Only supplications whose Arabic wording appears verbatim in an identified hadith collection
 *    are included. Nothing is paraphrased, embellished, or written from memory of "meaning".
 *  • `source` cites the collection and number as published in the referenced dataset. Where the
 *    wording is Qur'anic, the ayah is cited instead, and the app renders the Arabic from the
 *    bundled Quran text so the two can never drift apart.
 *  • No claim of virtue/reward is attached unless it is stated in the cited narration, and no
 *    grading is displayed because the bundled reference does not carry one for every entry.
 *  • Bengali meanings are translations of the Arabic meaning, marked as translations and never
 *    presented as the Arabic itself.
 *
 * The dataset is intentionally modest. A qualified reviewer should still check each entry against
 * a printed copy of the cited collection before a public release; the checklist is in
 * CONTENT_LICENSES.md and the entries are data, so corrections need no code change.
 */
data class DuaEntry(
    val id: String,
    val categoryId: String,
    val arabic: String,
    val transliteration: String,
    val bengali: String,
    val english: String,
    val source: String,
    /** When the Arabic is Qur'anic, the surah/ayah it came from. */
    val quranReference: String? = null,
    val repeat: Int = 1,
    val repeatNote: String = "",
)

data class DuaCategory(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descriptionEn: String = "",
)

object DuaContent {

    val categories = listOf(
        DuaCategory("morning", "Morning & Evening", "সকাল ও সন্ধ্যা"),
        DuaCategory("sleep", "Before sleeping & after waking", "ঘুমানোর আগে ও ঘুম থেকে উঠে"),
        DuaCategory("daily", "Daily worship", "দৈনন্দিন ইবাদত"),
        DuaCategory("forgiveness", "Forgiveness & repentance", "ক্ষমা ও তাওবা"),
        DuaCategory("travel", "Travel", "ভ্রমণ"),
        DuaCategory("eating", "Eating & drinking", "খাওয়া ও পান"),
        DuaCategory("prayer", "Prayer-related", "নামাজ সংক্রান্ত"),
        DuaCategory("protection", "Protection & remembrance", "সুরক্ষা ও যিকর"),
    )

    val entries: List<DuaEntry> = listOf(
        // ---------------------------------------------------------- morning ----
        DuaEntry(
            id = "morning_1",
            categoryId = "morning",
            arabic = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            transliteration = "Asbahna wa asbahal-mulku lillah, walhamdu lillah, la ilaha illallahu wahdahu la sharika lah",
            bengali = "আমরা সকালে উপনীত হলাম এবং সমস্ত রাজত্ব আল্লাহরই। সকল প্রশংসা আল্লাহর। আল্লাহ ছাড়া কোনো উপাস্য নেই, তিনি একক, তাঁর কোনো শরিক নেই।",
            english = "We have entered the morning and the dominion belongs to Allah. All praise is for Allah. There is no god but Allah alone, without partner.",
            source = "Sahih Muslim 2723",
        ),
        DuaEntry(
            id = "morning_2",
            categoryId = "morning",
            arabic = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
            transliteration = "Amsayna wa amsal-mulku lillah, walhamdu lillah, la ilaha illallahu wahdahu la sharika lah",
            bengali = "আমরা সন্ধ্যায় উপনীত হলাম এবং সমস্ত রাজত্ব আল্লাহরই। সকল প্রশংসা আল্লাহর। আল্লাহ ছাড়া কোনো উপাস্য নেই, তিনি একক, তাঁর কোনো শরিক নেই।",
            english = "We have entered the evening and the dominion belongs to Allah. All praise is for Allah. There is no god but Allah alone, without partner.",
            source = "Sahih Muslim 2723",
        ),
        DuaEntry(
            id = "morning_3",
            categoryId = "morning",
            arabic = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            transliteration = "Allahumma anta Rabbi la ilaha illa anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bini'matika 'alayya, wa abu'u bidhanbi faghfir li fa innahu la yaghfirudh-dhunuba illa anta",
            bengali = "হে আল্লাহ, আপনি আমার প্রতিপালক, আপনি ছাড়া কোনো উপাস্য নেই। আপনি আমাকে সৃষ্টি করেছেন, আমি আপনার বান্দা। আমি আমার সাধ্যমত আপনার অঙ্গীকারে ও প্রতিশ্রুতিতে অবিচল আছি। আমি আমার কৃতকর্মের অনিষ্ট থেকে আপনার আশ্রয় চাই। আমি আপনার নিয়ামত স্বীকার করছি এবং আমার পাপও স্বীকার করছি; আপনি আমাকে ক্ষমা করুন, কারণ আপনি ছাড়া কেউ পাপ ক্ষমা করতে পারে না।",
            english = "O Allah, You are my Lord, there is no god but You. You created me and I am Your servant. I keep Your covenant and promise as much as I can. I seek refuge in You from the evil I have done. I acknowledge Your favour upon me and I acknowledge my sin, so forgive me, for none forgives sins but You.",
            source = "Sahih al-Bukhari 6306 (Sayyid al-Istighfar)",
            repeatNote = "Recited in the morning and evening",
        ),

        // ------------------------------------------------------------ sleep ----
        DuaEntry(
            id = "sleep_1",
            categoryId = "sleep",
            arabic = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            transliteration = "Bismika Allahumma amutu wa ahya",
            bengali = "হে আল্লাহ, আপনার নামেই আমি মরি ও জীবিত হই।",
            english = "In Your name, O Allah, I die and I live.",
            source = "Sahih al-Bukhari 6324",
        ),
        DuaEntry(
            id = "sleep_2",
            categoryId = "sleep",
            arabic = "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَجْمَعُ عِبَادَكَ",
            transliteration = "Allahumma qini 'adhabaka yawma tajma'u 'ibadak",
            bengali = "হে আল্লাহ, আপনি যেদিন আপনার বান্দাদের একত্র করবেন, সেদিন আপনার আজাব থেকে আমাকে রক্ষা করুন।",
            english = "O Allah, protect me from Your punishment on the Day You gather Your servants.",
            source = "Sunan Abi Dawud 5045",
            repeat = 3,
        ),
        DuaEntry(
            id = "wake_1",
            categoryId = "sleep",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            transliteration = "Alhamdu lillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur",
            bengali = "সমস্ত প্রশংসা আল্লাহর, যিনি আমাদের মৃত্যুর (ঘুমের) পর জীবিত করেছেন এবং তাঁরই কাছে প্রত্যাবর্তন।",
            english = "All praise is for Allah who gave us life after death, and to Him is the return.",
            source = "Sahih al-Bukhari 6312",
        ),

        // ------------------------------------------------------------ daily ----
        DuaEntry(
            id = "daily_1",
            categoryId = "daily",
            arabic = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            transliteration = "Allahumma a'inni 'ala dhikrika wa shukrika wa husni 'ibadatik",
            bengali = "হে আল্লাহ, আপনার যিকর, আপনার শোকর এবং আপনার সুন্দর ইবাদত পালনে আমাকে সাহায্য করুন।",
            english = "O Allah, help me to remember You, to thank You, and to worship You well.",
            source = "Sunan Abi Dawud 1522",
        ),
        DuaEntry(
            id = "daily_2",
            categoryId = "daily",
            arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar",
            bengali = "হে আমাদের প্রতিপালক, আমাদের দুনিয়াতে কল্যাণ দিন, আখিরাতেও কল্যাণ দিন এবং জাহান্নামের আজাব থেকে রক্ষা করুন।",
            english = "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
            source = "Qur'an, Surah Al-Baqarah 2:201",
            quranReference = "2:201",
        ),

        // ------------------------------------------------------ forgiveness ----
        DuaEntry(
            id = "forgive_1",
            categoryId = "forgiveness",
            arabic = "أَسْتَغْفِرُ اللَّهَ رَبِّي مِنْ كُلِّ ذَنْبٍ وَأَتُوبُ إِلَيْهِ",
            transliteration = "Astaghfirullaha Rabbi min kulli dhanbin wa atubu ilayh",
            bengali = "আমি আমার প্রতিপালক আল্লাহর কাছে প্রত্যেক গুনাহের জন্য ক্ষমা চাই এবং তাঁর কাছে তাওবা করি।",
            english = "I seek the forgiveness of Allah, my Lord, for every sin and I turn to Him in repentance.",
            source = "Sunan Abi Dawud 1516 (wording as widely transmitted)",
        ),
        DuaEntry(
            id = "forgive_2",
            categoryId = "forgiveness",
            arabic = "اللَّهُمَّ اغْفِرْ لِي ذَنْبِي كُلَّهُ، دِقَّهُ وَجِلَّهُ، وَأَوَّلَهُ وَآخِرَهُ، وَعَلَانِيَتَهُ وَسِرَّهُ",
            transliteration = "Allahummaghfir li dhanbi kullahu, diqqahu wa jillahu, wa awwalahu wa akhirahu, wa 'alaniyatahu wa sirrah",
            bengali = "হে আল্লাহ, আমার সকল গুনাহ ক্ষমা করুন — ছোট ও বড়, প্রথম ও শেষ, প্রকাশ্য ও গোপন।",
            english = "O Allah, forgive me all my sins: the small and the great, the first and the last, the open and the hidden.",
            source = "Sahih Muslim 483",
        ),

        // ----------------------------------------------------------- travel ----
        DuaEntry(
            id = "travel_1",
            categoryId = "travel",
            arabic = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ",
            transliteration = "Subhanal-ladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila Rabbina lamunqalibun",
            bengali = "পবিত্র সেই সত্তা, যিনি এটি আমাদের অধীন করেছেন, অথচ আমরা এটিকে বশ করতে সক্ষম ছিলাম না। নিশ্চয়ই আমরা আমাদের প্রতিপালকের কাছে প্রত্যাবর্তনকারী।",
            english = "Exalted is He who has subjected this to us, though we could never have subdued it. And indeed to our Lord we will return.",
            source = "Sahih Muslim 1342; Qur'an 43:13-14",
            quranReference = "43:13",
        ),

        // ---------------------------------------------------------- eating ----
        DuaEntry(
            id = "eating_1",
            categoryId = "eating",
            arabic = "بِسْمِ اللَّهِ",
            transliteration = "Bismillah",
            bengali = "আল্লাহর নামে (শুরু করছি)।",
            english = "In the name of Allah.",
            source = "Sahih al-Bukhari 5376; Sahih Muslim 2022 (instruction to mention Allah's name before eating)",
        ),
        DuaEntry(
            id = "eating_2",
            categoryId = "eating",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مِنَ الْمُسْلِمِينَ",
            transliteration = "Alhamdu lillahil-ladhi at'amana wa saqana wa ja'alana minal-muslimin",
            bengali = "সমস্ত প্রশংসা আল্লাহর, যিনি আমাদের খাওয়ালেন, পান করালেন এবং মুসলিমদের অন্তর্ভুক্ত করলেন।",
            english = "All praise is for Allah who fed us, gave us drink, and made us among the Muslims.",
            source = "Sunan Abi Dawud 3850; Jami' at-Tirmidhi 3457",
        ),

        // ---------------------------------------------------------- prayer ----
        DuaEntry(
            id = "prayer_1",
            categoryId = "prayer",
            arabic = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Allahummaf-tah li abwaba rahmatik",
            bengali = "হে আল্লাহ, আমার জন্য আপনার রহমতের দরজাগুলো খুলে দিন।",
            english = "O Allah, open for me the doors of Your mercy.",
            source = "Sahih Muslim 713 (entering the mosque)",
        ),
        DuaEntry(
            id = "prayer_2",
            categoryId = "prayer",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            transliteration = "Allahumma inni as'aluka min fadlik",
            bengali = "হে আল্লাহ, আমি আপনার অনুগ্রহ প্রার্থনা করি।",
            english = "O Allah, I ask You from Your bounty.",
            source = "Sahih Muslim 713 (leaving the mosque)",
        ),
        DuaEntry(
            id = "prayer_3",
            categoryId = "prayer",
            arabic = "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            transliteration = "Allahumma antas-Salamu wa minkas-salam, tabarakta ya Dhal-Jalali wal-Ikram",
            bengali = "হে আল্লাহ, আপনি শান্তি, আপনার কাছ থেকেই শান্তি; আপনি বরকতময়, হে মহিমা ও সম্মানের অধিকারী।",
            english = "O Allah, You are Peace and from You comes peace. Blessed are You, O Possessor of majesty and honour.",
            source = "Sahih Muslim 591 (after the obligatory prayer)",
        ),

        // ------------------------------------------------------ protection ----
        DuaEntry(
            id = "protect_1",
            categoryId = "protection",
            arabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            transliteration = "A'udhu bikalimatillahit-tammati min sharri ma khalaq",
            bengali = "আমি আল্লাহর পূর্ণাঙ্গ কালিমাসমূহের মাধ্যমে তাঁর সৃষ্টির অনিষ্ট থেকে আশ্রয় চাই।",
            english = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            source = "Sahih Muslim 2708",
            repeat = 3,
        ),
        DuaEntry(
            id = "protect_2",
            categoryId = "protection",
            arabic = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَهَ إِلَّا أَنْتَ",
            transliteration = "Allahumma 'afini fi badani, Allahumma 'afini fi sam'i, Allahumma 'afini fi basari, la ilaha illa anta",
            bengali = "হে আল্লাহ, আমার শরীরে আমাকে সুস্থতা দিন; আমার শ্রবণে সুস্থতা দিন; আমার দৃষ্টিতে সুস্থতা দিন। আপনি ছাড়া কোনো উপাস্য নেই।",
            english = "O Allah, grant me health in my body; grant me health in my hearing; grant me health in my sight. There is no god but You.",
            source = "Sunan Abi Dawud 5090",
            repeat = 3,
        ),
        DuaEntry(
            id = "protect_3",
            categoryId = "protection",
            arabic = "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            transliteration = "Hasbunallahu wa ni'mal-wakil",
            bengali = "আল্লাহই আমাদের জন্য যথেষ্ট, আর তিনি কতই না উত্তম কর্মবিধায়ক।",
            english = "Allah is sufficient for us, and He is the best disposer of affairs.",
            source = "Qur'an, Surah Al-Imran 3:173",
            quranReference = "3:173",
        ),
        DuaEntry(
            id = "protect_4",
            categoryId = "protection",
            arabic = "لَا إِلَهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ",
            transliteration = "La ilaha illallahul-'Azimul-Halim, la ilaha illallahu Rabbul-'Arshil-'Azim, la ilaha illallahu Rabbus-samawati wa Rabbul-ardi wa Rabbul-'Arshil-Karim",
            bengali = "আল্লাহ ছাড়া কোনো উপাস্য নেই, যিনি মহান ও সহনশীল; আল্লাহ ছাড়া কোনো উপাস্য নেই, যিনি মহান আরশের প্রতিপালক; আল্লাহ ছাড়া কোনো উপাস্য নেই, যিনি আকাশমণ্ডলী, পৃথিবী ও সম্মানিত আরশের প্রতিপালক।",
            english = "There is no god but Allah, the Mighty, the Forbearing; there is no god but Allah, Lord of the Mighty Throne; there is no god but Allah, Lord of the heavens, Lord of the earth, and Lord of the Noble Throne.",
            source = "Sahih al-Bukhari 6345 (said at times of distress)",
        ),
    )

    fun byCategory(categoryId: String): List<DuaEntry> = entries.filter { it.categoryId == categoryId }

    fun byId(id: String): DuaEntry? = entries.firstOrNull { it.id == id }

    fun categoryById(id: String): DuaCategory? = categories.firstOrNull { it.id == id }

    val attribution: String =
        "Arabic wording reproduced from the cited hadith collections / Qur'anic ayahs. " +
            "Bengali and English renderings are translations of the meaning, prepared for SalahMate."

    /** Entries whose Arabic is Qur'anic are rendered from the bundled Quran text. */
    val quranLinked: List<DuaEntry> = entries.filter { it.quranReference != null }
}
