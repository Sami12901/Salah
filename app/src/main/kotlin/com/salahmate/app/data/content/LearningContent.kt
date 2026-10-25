package com.salahmate.app.data.content

/** One of the 99 names of Allah with its transliteration and Bengali meaning. */
data class DivineName(val number: Int, val arabic: String, val transliteration: String, val bengali: String)

/** Six Kalimas — the formulations commonly taught in Bangladeshi/South Asian curricula. */
data class Kalima(val number: Int, val titleEn: String, val titleBn: String, val arabic: String, val transliteration: String, val bengali: String)

data class GuideStep(val titleEn: String, val titleBn: String, val bodyEn: String, val bodyBn: String)

/**
 * Reviewed learning content. Attribution and review notes live in CONTENT_LICENSES.md.
 *
 * • Asma ul Husna: the standard 99 names as listed in the widely circulated Tirmidhi narration.
 *   Transliterations are simplified for Bengali readers; meanings are translations, not claims
 *   about Allah's attributes beyond what the names denote.
 * • Kalimas: devotional formulations used in teaching. They are *not* a single hadith; the app
 *   labels them as commonly taught formulations rather than attributing them to a narration.
 * • Ayat al-Kursi is not duplicated here: the learning screen renders Qur'an 2:255 directly from
 *   the bundled Quran text so the Arabic can never diverge from the mushaf text.
 * • Wudu guide: described from Qur'an 5:6 and the general Prophetic description of ablution.
 */
object LearningContent {

    val divineNames: List<DivineName> = listOf(
        DivineName(1, "الرَّحْمَنُ", "Ar-Rahman", "পরম দয়ালু"),
        DivineName(2, "الرَّحِيمُ", "Ar-Rahim", "পরম করুণাময়"),
        DivineName(3, "الْمَلِكُ", "Al-Malik", "সার্বভৌম অধিপতি"),
        DivineName(4, "الْقُدُّوسُ", "Al-Quddus", "পবিত্র"),
        DivineName(5, "السَّلَامُ", "As-Salam", "শান্তিদাতা"),
        DivineName(6, "الْمُؤْمِنُ", "Al-Mu'min", "নিরাপত্তাদাতা"),
        DivineName(7, "الْمُهَيْمِنُ", "Al-Muhaymin", "রক্ষণাবেক্ষণকারী"),
        DivineName(8, "الْعَزِيزُ", "Al-Aziz", "পরাক্রমশালী"),
        DivineName(9, "الْجَبَّارُ", "Al-Jabbar", "মহাপ্রতাপশালী"),
        DivineName(10, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "মহিমান্বিত"),
        DivineName(11, "الْخَالِقُ", "Al-Khaliq", "সৃষ্টিকর্তা"),
        DivineName(12, "الْبَارِئُ", "Al-Bari'", "উদ্ভাবক"),
        DivineName(13, "الْمُصَوِّرُ", "Al-Musawwir", "আকৃতিদানকারী"),
        DivineName(14, "الْغَفَّارُ", "Al-Ghaffar", "অত্যন্ত ক্ষমাশীল"),
        DivineName(15, "الْقَهَّارُ", "Al-Qahhar", "প্রবল প্রতাপশালী"),
        DivineName(16, "الْوَهَّابُ", "Al-Wahhab", "অনুগ্রহদাতা"),
        DivineName(17, "الرَّزَّاقُ", "Ar-Razzaq", "রিযিকদাতা"),
        DivineName(18, "الْفَتَّاحُ", "Al-Fattah", "উদারভাবে দানকারী, দরজা-খুলে-দেন"),
        DivineName(19, "الْعَلِيمُ", "Al-Alim", "সর্বজ্ঞ"),
        DivineName(20, "الْقَابِضُ", "Al-Qabid", "সংকুচিতকারী"),
        DivineName(21, "الْبَاسِطُ", "Al-Basit", "প্রসারিতকারী"),
        DivineName(22, "الْخَافِضُ", "Al-Khafid", "নিম্নমুখীকারী"),
        DivineName(23, "الرَّافِعُ", "Ar-Rafi'", "উন্নীতকারী"),
        DivineName(24, "الْمُعِزُّ", "Al-Mu'izz", "সম্মানদাতা"),
        DivineName(25, "الْمُذِلُّ", "Al-Mudhill", "নীচুকারী"),
        DivineName(26, "السَّمِيعُ", "As-Sami'", "সর্বশ্রোতা"),
        DivineName(27, "الْبَصِيرُ", "Al-Basir", "সর্বদ্রষ্টা"),
        DivineName(28, "الْحَكَمُ", "Al-Hakam", "বিচারক"),
        DivineName(29, "الْعَدْلُ", "Al-'Adl", "ন্যায়বিচারক"),
        DivineName(30, "اللَّطِيفُ", "Al-Latif", "সূক্ষ্মদর্শী ও কোমল"),
        DivineName(31, "الْخَبِيرُ", "Al-Khabir", "সর্বজ্ঞাতা"),
        DivineName(32, "الْحَلِيمُ", "Al-Halim", "সহনশীল"),
        DivineName(33, "الْعَظِيمُ", "Al-Azim", "মহিমান্বিত"),
        DivineName(34, "الْغَفُورُ", "Al-Ghafur", "ক্ষমাশীল"),
        DivineName(35, "الشَّكُورُ", "Ash-Shakur", "গুণগ্রাহী"),
        DivineName(36, "الْعَلِيُّ", "Al-Ali", "সুউচ্চ"),
        DivineName(37, "الْكَبِيرُ", "Al-Kabir", "মহান"),
        DivineName(38, "الْحَفِيظُ", "Al-Hafiz", "সংরক্ষণকারী"),
        DivineName(39, "الْمُقِيتُ", "Al-Muqit", "রিযিক ও শক্তি দানকারী"),
        DivineName(40, "الْحَسِيبُ", "Al-Hasib", "হিসাব গ্রহণকারী"),
        DivineName(41, "الْجَلِيلُ", "Al-Jalil", "মহিমান্বিত"),
        DivineName(42, "الْكَرِيمُ", "Al-Karim", "মহাদানশীল"),
        DivineName(43, "الرَّقِيبُ", "Ar-Raqib", "পর্যবেক্ষণকারী"),
        DivineName(44, "الْمُجِيبُ", "Al-Mujib", "প্রার্থনা-শ্রবণকারী"),
        DivineName(45, "الْوَاسِعُ", "Al-Wasi'", "সর্বব্যাপী"),
        DivineName(46, "الْحَكِيمُ", "Al-Hakim", "প্রজ্ঞাময়"),
        DivineName(47, "الْوَدُودُ", "Al-Wadud", "প্রেমময়"),
        DivineName(48, "الْمَجِيدُ", "Al-Majid", "মহিমান্বিত"),
        DivineName(49, "الْبَاعِثُ", "Al-Ba'ith", "পুনরুত্থানকারী"),
        DivineName(50, "الشَّهِيدُ", "Ash-Shahid", "সাক্ষী"),
        DivineName(51, "الْحَقُّ", "Al-Haqq", "সত্য"),
        DivineName(52, "الْوَكِيلُ", "Al-Wakil", "কর্মবিধায়ক"),
        DivineName(53, "الْقَوِيُّ", "Al-Qawiyy", "শক্তিশালী"),
        DivineName(54, "الْمَتِينُ", "Al-Matin", "দৃঢ়"),
        DivineName(55, "الْوَلِيُّ", "Al-Waliyy", "অভিভাবক"),
        DivineName(56, "الْحَمِيدُ", "Al-Hamid", "প্রশংসিত"),
        DivineName(57, "الْمُحْصِي", "Al-Muhsi", "সবকিছুর গণনাকারী"),
        DivineName(58, "الْمُبْدِئُ", "Al-Mubdi'", "সৃষ্টির সূচনাকারী"),
        DivineName(59, "الْمُعِيدُ", "Al-Mu'id", "পুনরায় সৃষ্টিকারী"),
        DivineName(60, "الْمُحْيِي", "Al-Muhyi", "জীবনদাতা"),
        DivineName(61, "الْمُمِيتُ", "Al-Mumit", "মৃত্যুদানকারী"),
        DivineName(62, "الْحَيُّ", "Al-Hayy", "চিরঞ্জীব"),
        DivineName(63, "الْقَيُّومُ", "Al-Qayyum", "সর্বসত্তার ধারক"),
        DivineName(64, "الْوَاجِدُ", "Al-Wajid", "প্রাচুর্যময়"),
        DivineName(65, "الْمَاجِدُ", "Al-Majid", "মহিমান্বিত"),
        DivineName(66, "الْوَاحِدُ", "Al-Wahid", "এক"),
        DivineName(67, "الْأَحَدُ", "Al-Ahad", "অদ্বিতীয়"),
        DivineName(68, "الصَّمَدُ", "As-Samad", "অমুখাপেক্ষী"),
        DivineName(69, "الْقَادِرُ", "Al-Qadir", "সর্বশক্তিমান"),
        DivineName(70, "الْمُقْتَدِرُ", "Al-Muqtadir", "নির্ধারক"),
        DivineName(71, "الْمُقَدِّمُ", "Al-Muqaddim", "অগ্রবর্তীকারী"),
        DivineName(72, "الْمُؤَخِّرُ", "Al-Mu'akhkhir", "পশ্চাদ্ববর্তীকারী"),
        DivineName(73, "الْأَوَّلُ", "Al-Awwal", "প্রথম"),
        DivineName(74, "الْآخِرُ", "Al-Akhir", "শেষ"),
        DivineName(75, "الظَّاهِرُ", "Az-Zahir", "প্রকাশ্য"),
        DivineName(76, "الْبَاطِنُ", "Al-Batin", "গোপন"),
        DivineName(77, "الْوَالِي", "Al-Wali", "পরিচালক"),
        DivineName(78, "الْمُتَعَالِي", "Al-Muta'ali", "সর্বোচ্চ"),
        DivineName(79, "الْبَرُّ", "Al-Barr", "পরম সৎ"),
        DivineName(80, "التَّوَّابُ", "At-Tawwab", "তাওবা গ্রহণকারী"),
        DivineName(81, "الْمُنْتَقِمُ", "Al-Muntaqim", "প্রতিশোধগ্রহণকারী"),
        DivineName(82, "الْعَفُوُّ", "Al-'Afuww", "ক্ষমাশীল"),
        DivineName(83, "الرَّءُوفُ", "Ar-Ra'uf", "পরম স্নেহশীল"),
        DivineName(84, "مَالِكُ الْمُلْكِ", "Malik-ul-Mulk", "রাজত্বের অধিপতি"),
        DivineName(85, "ذُو الْجَلَالِ وَالْإِكْرَامِ", "Dhul-Jalali wal-Ikram", "মহিমা ও সম্মানের অধিকারী"),
        DivineName(86, "الْمُقْسِطُ", "Al-Muqsit", "ন্যায়পরায়ণ"),
        DivineName(87, "الْجَامِعُ", "Al-Jami'", "একত্রকারী"),
        DivineName(88, "الْغَنِيُّ", "Al-Ghani", "অমুখাপেক্ষী"),
        DivineName(89, "الْمُغْنِي", "Al-Mughni", "সমৃদ্ধিদানকারী"),
        DivineName(90, "الْمَانِعُ", "Al-Mani'", "নিবারক"),
        DivineName(91, "الضَّارُّ", "Ad-Darr", "ক্ষতিসাধক"),
        DivineName(92, "النَّافِعُ", "An-Nafi'", "উপকারকারী"),
        DivineName(93, "النُّورُ", "An-Nur", "আলো"),
        DivineName(94, "الْهَادِي", "Al-Hadi", "পথপ্রদর্শক"),
        DivineName(95, "الْبَدِيعُ", "Al-Badi'", "অনন্য সৃষ্টিকারী"),
        DivineName(96, "الْبَاقِي", "Al-Baqi", "চিরস্থায়ী"),
        DivineName(97, "الْوَارِثُ", "Al-Warith", "উত্তরাধিকারী"),
        DivineName(98, "الرَّشِيدُ", "Ar-Rashid", "সঠিক পথপ্রদর্শক"),
        DivineName(99, "الصَّبُورُ", "As-Sabur", "ধৈর্যশালী"),
    )

    val kalimas: List<Kalima> = listOf(
        Kalima(
            1, "Kalima Tayyibah", "কালিমা তাইয়্যিবা",
            "لَا إِلَهَ إِلَّا اللَّهُ مُحَمَّدٌ رَسُولُ اللَّهِ",
            "La ilaha illallahu Muhammadur rasulullah",
            "আল্লাহ ছাড়া কোনো উপাস্য নেই, মুহাম্মদ (সা.) আল্লাহর রাসূল।",
        ),
        Kalima(
            2, "Kalima Shahadah", "কালিমা শাহাদাত",
            "أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
            "Ash-hadu an la ilaha illallahu wahdahu la sharika lahu wa ash-hadu anna Muhammadan 'abduhu wa rasuluh",
            "আমি সাক্ষ্য দিচ্ছি যে, আল্লাহ ছাড়া কোনো উপাস্য নেই, তিনি একক, তাঁর কোনো শরিক নেই; এবং আমি সাক্ষ্য দিচ্ছি যে, মুহাম্মদ (সা.) তাঁর বান্দা ও রাসূল।",
        ),
        Kalima(
            3, "Kalima Tamjeed", "কালিমা তামজীদ",
            "سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ",
            "SubhanAllahi walhamdu lillahi wa la ilaha illallahu wallahu akbar, wa la hawla wa la quwwata illa billahil-'Aliyyil-'Azim",
            "আল্লাহ পবিত্র, সকল প্রশংসা আল্লাহর, আল্লাহ ছাড়া কোনো উপাস্য নেই, আল্লাহ সর্বশ্রেষ্ঠ; এবং মহান আল্লাহর সাহায্য ছাড়া কোনো শক্তি ও সামর্থ্য নেই।",
        ),
        Kalima(
            4, "Kalima Tawheed", "কালিমা তাওহীদ",
            "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ وَهُوَ حَيٌّ لَا يَمُوتُ أَبَدًا أَبَدًا ذُو الْجَلَالِ وَالْإِكْرَامِ بِيَدِهِ الْخَيْرُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "La ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu yuhyi wa yumitu wa huwa hayyun la yamutu abadan abada, Dhul-Jalali wal-Ikram, biyadihil-khayr wa huwa 'ala kulli shay'in qadir",
            "আল্লাহ ছাড়া কোনো উপাস্য নেই, তিনি একক, তাঁর কোনো শরিক নেই; রাজত্ব ও প্রশংসা তাঁরই; তিনি জীবন দেন ও মৃত্যু দেন; তিনি চিরঞ্জীব, কখনো মৃত্যুবরণ করবেন না; তিনি মহিমা ও সম্মানের অধিকারী; কল্যাণ তাঁরই হাতে এবং তিনি সব কিছুর ওপর ক্ষমতাবান।",
        ),
        Kalima(
            5, "Kalima Istighfar", "কালিমা ইস্তিগফার",
            "أَسْتَغْفِرُ اللَّهَ رَبِّي مِنْ كُلِّ ذَنْبٍ أَذْنَبْتُهُ عَمْدًا أَوْ خَطَأً سِرًّا أَوْ عَلَانِيَةً وَأَتُوبُ إِلَيْهِ",
            "Astaghfirullaha Rabbi min kulli dhanbin adhnabtuhu 'amdan aw khata'an sirran aw 'alaniyatan wa atubu ilayh",
            "আমি আমার প্রতিপালক আল্লাহর কাছে ক্ষমা চাই — প্রত্যেক সেই গুনাহের জন্য যা আমি জেনে বা ভুলে, গোপনে বা প্রকাশ্যে করেছি; এবং আমি তাঁর কাছে তাওবা করি।",
        ),
        Kalima(
            6, "Kalima Radd-e-Kufr", "কালিমা রদ্দে কুফর",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ أَنْ أُشْرِكَ بِكَ شَيْئًا وَأَنَا أَعْلَمُ بِهِ وَأَسْتَغْفِرُكَ لِمَا لَا أَعْلَمُ بِهِ تُبْتُ عَنْهُ وَتَبَرَّأْتُ مِنَ الْكُفْرِ وَالشِّرْكِ وَالْكِذْبِ وَالْغِيبَةِ وَالْبِدْعَةِ وَالنَّمِيمَةِ وَالْفَوَاحِشِ وَالْبُهْتَانِ وَالْمَعَاصِي كُلِّهَا وَأَسْلَمْتُ وَأَقُولُ لَا إِلَهَ إِلَّا اللَّهُ مُحَمَّدٌ رَسُولُ اللَّهِ",
            "Allahumma inni a'udhu bika min an ushrika bika shay'an wa ana a'lamu bihi wa astaghfiruka lima la a'lamu bihi tubtu 'anhu wa tabarra'tu minal-kufri wash-shirki wal-kidhbi wal-ghibati wal-bid'ati wan-namimati wal-fawahishi wal-buhtani wal-ma'asi kulliha wa aslamtu wa aqulu la ilaha illallahu Muhammadur rasulullah",
            "হে আল্লাহ, আমি জেনে আপনার সাথে কাউকে শরিক করা থেকে আপনার আশ্রয় চাই; এবং না জেনে যা করেছি তার জন্য ক্ষমা চাই। আমি তা থেকে তাওবা করলাম এবং কুফর, শিরক, মিথ্যা, গীবত, বিদআত, চোগলখুরি, অশ্লীলতা, অপবাদ ও সকল গুনাহ থেকে মুক্ত হলাম। আমি আত্মসমর্পণ করলাম এবং বলি: আল্লাহ ছাড়া কোনো উপাস্য নেই, মুহাম্মদ (সা.) আল্লাহর রাসূল।",
        ),
    )

    val wuduSteps: List<GuideStep> = listOf(
        GuideStep(
            "Intention and Bismillah", "নিয়ত ও বিসমিল্লাহ",
            "Make the intention (niyyah) for purification and begin by mentioning Allah's name.",
            "পবিত্রতার নিয়ত করুন এবং আল্লাহর নাম (বিসমিল্লাহ) দিয়ে শুরু করুন।",
        ),
        GuideStep(
            "Wash the hands", "হাত ধোয়া",
            "Wash both hands up to the wrists, three times, including between the fingers.",
            "দুই হাত কব্জি পর্যন্ত তিনবার ধৌত করুন, আঙুলের ফাঁকগুলোও ভিজিয়ে নিন।",
        ),
        GuideStep(
            "Rinse the mouth", "কুলি করা",
            "Take water into the mouth and rinse it, three times.",
            "মুখে পানি দিয়ে তিনবার কুলি করুন।",
        ),
        GuideStep(
            "Clean the nose", "নাক পরিষ্কার করা",
            "Sniff water into the nose and blow it out, three times, gently.",
            "আলতোভাবে নাকে পানি টেনে তিনবার পরিষ্কার করুন।",
        ),
        GuideStep(
            "Wash the face", "মুখমণ্ডল ধোয়া",
            "Wash the whole face from the forehead to the chin and ear to ear, three times.",
            "কপাল থেকে চিবুক এবং এক কান থেকে অন্য কান পর্যন্ত সম্পূর্ণ মুখমণ্ডল তিনবার ধৌত করুন।",
        ),
        GuideStep(
            "Wash the arms", "হাত-বাহু ধোয়া",
            "Wash the right arm up to and including the elbow three times, then the left arm.",
            "ডান হাত কনুইসহ তিনবার ধৌত করুন, এরপর বাম হাত একইভাবে।",
        ),
        GuideStep(
            "Wipe the head", "মাথা মাসেহ করা",
            "Wipe the whole head with wet hands, front to back and back to front, once.",
            "ভেজা হাতে একবার মাথা মাসেহ করুন — সামনে থেকে পেছনে এবং পেছনে থেকে সামনে।",
        ),
        GuideStep(
            "Wipe the ears", "কান মাসেহ করা",
            "Wipe the inside of both ears with the index fingers and the back with the thumbs.",
            "শাহাদাত আঙুল দিয়ে দুই কানের ভেতর ও বুড়ো আঙুল দিয়ে পেছনভাগ মাসেহ করুন।",
        ),
        GuideStep(
            "Wash the feet", "পা ধোয়া",
            "Wash the right foot up to and including the ankle three times, then the left.",
            "ডান পা টাখনুসহ তিনবার ধৌত করুন, এরপর বাম পা।",
        ),
    )

    val wuduReference: String =
        "Obligation of ablution: Qur'an 5:6. The step sequence above follows the well-known " +
            "description of the Prophet's ablution transmitted in the hadith collections; the app " +
            "keeps the steps generic rather than quoting a single narration."
}
