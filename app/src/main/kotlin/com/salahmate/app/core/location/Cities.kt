package com.salahmate.app.core.location

/**
 * Curated city list for manual location selection — the app must never require GPS.
 *
 * Coordinates are city-centre values from public gazetteers (OpenStreetMap/Wikipedia
 * coordinates), rounded to 3 dp (~110 m). For prayer times this precision is far below the
 * rounding of the output itself, but the list is deliberately small and hand-checked rather than
 * an unbounded geocoding result set. Users can always enter exact coordinates manually.
 */
data class City(
    val nameEn: String,
    val nameBn: String,
    val countryEn: String,
    val countryBn: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
) {
    val label: String get() = "$nameEn, $countryEn"
    val labelBn: String get() = "$nameBn, $countryBn"
}

object Cities {

    private fun bd(en: String, bn: String, lat: Double, lon: Double) =
        City(en, bn, "Bangladesh", "বাংলাদেশ", lat, lon, "Asia/Dhaka")

    val bangladesh: List<City> = listOf(
        bd("Dhaka", "ঢাকা", 23.8103, 90.4125),
        bd("Chattogram", "চট্টগ্রাম", 22.3569, 91.7832),
        bd("Sylhet", "সিলেট", 24.8949, 91.8687),
        bd("Khulna", "খুলনা", 22.8456, 89.5403),
        bd("Rajshahi", "রাজশাহী", 24.3636, 88.6241),
        bd("Barishal", "বরিশাল", 22.7010, 90.3535),
        bd("Rangpur", "রংপুর", 25.7439, 89.2752),
        bd("Mymensingh", "ময়মনসিংহ", 24.7471, 90.4203),
        bd("Cumilla", "কুমিল্লা", 23.4607, 91.1809),
        bd("Narayanganj", "নারায়ণগঞ্জ", 23.6238, 90.5000),
        bd("Gazipur", "গাজীপুর", 23.9999, 90.4203),
        bd("Bogura", "বগুড়া", 24.8465, 89.3773),
        bd("Jashore", "যশোর", 23.1664, 89.2081),
        bd("Cox's Bazar", "কক্সবাজার", 21.4272, 92.0058),
        bd("Dinajpur", "দিনাজপুর", 25.6217, 88.6354),
        bd("Faridpur", "ফরিদপুর", 23.6070, 89.8429),
        bd("Noakhali", "নোয়াখালী", 22.8696, 91.0995),
        bd("Pabna", "পাবনা", 24.0064, 89.2372),
        bd("Tangail", "টাঙ্গাইল", 24.2513, 89.9167),
        bd("Kushtia", "কুষ্টিয়া", 23.9013, 89.1206),
        bd("Feni", "ফেনী", 23.0159, 91.3976),
        bd("Brahmanbaria", "ব্রাহ্মণবাড়িয়া", 23.9571, 91.1115),
        bd("Jamalpur", "জামালপুর", 24.9375, 89.9372),
        bd("Sirajganj", "সিরাজগঞ্জ", 24.4534, 89.7007),
        bd("Chandpur", "চাঁদপুর", 23.2333, 90.6712),
        bd("Satkhira", "সাতক্ষীরা", 22.7185, 89.0705),
        bd("Bagerhat", "বাগেরহাট", 22.6516, 89.7859),
        bd("Patuakhali", "পটুয়াখালী", 22.3596, 90.3299),
        bd("Rangamati", "রাঙ্গামাটি", 22.6533, 92.1735),
        bd("Bandarban", "বান্দরবান", 22.1953, 92.2184),
        bd("Sunamganj", "সুনামগঞ্জ", 25.0658, 91.3950),
        bd("Habiganj", "হবিগঞ্জ", 24.3745, 91.4155),
        bd("Moulvibazar", "মৌলভীবাজার", 24.4829, 91.7774),
        bd("Kishoreganj", "কিশোরগঞ্জ", 24.4449, 90.7766),
        bd("Narsingdi", "নরসিংদী", 23.9322, 90.7150),
        bd("Gopalganj", "গোপালগঞ্জ", 23.0051, 89.8266),
        bd("Lalmonirhat", "লালমনিরহাট", 25.9923, 89.2847),
        bd("Kurigram", "কুড়িগ্রাম", 25.8072, 89.6295),
        bd("Gaibandha", "গাইবান্ধা", 25.3288, 89.5286),
        bd("Nilphamari", "নীলফামারী", 25.9310, 88.8560),
        bd("Panchagarh", "পঞ্চগড়", 26.3411, 88.5542),
        bd("Thakurgaon", "ঠাকুরগাঁও", 26.0337, 88.4616),
        bd("Joypurhat", "জয়পুরহাট", 25.0947, 89.0227),
        bd("Naogaon", "নওগাঁ", 24.7936, 88.9318),
        bd("Natore", "নাটোর", 24.4206, 89.0003),
        bd("Chapainawabganj", "চাঁপাইনবাবগঞ্জ", 24.5965, 88.2775),
        bd("Sherpur", "শেরপুর", 25.0205, 90.0153),
        bd("Madaripur", "মাদারীপুর", 23.1640, 90.1897),
        bd("Pirojpur", "পিরোজপুর", 22.5841, 89.9720),
        bd("Barguna", "বরগুনা", 22.1596, 90.1263),
        bd("Chuadanga", "চুয়াডাঙ্গা", 23.6402, 88.8418),
        bd("Jhenaidah", "ঝিনাইদহ", 23.5448, 89.1539),
    )

    val international: List<City> = listOf(
        City("Makkah", "মক্কা", "Saudi Arabia", "সৌদি আরব", 21.4225, 39.8262, "Asia/Riyadh"),
        City("Madinah", "মদিনা", "Saudi Arabia", "সৌদি আরব", 24.4672, 39.6111, "Asia/Riyadh"),
        City("Riyadh", "রিয়াদ", "Saudi Arabia", "সৌদি আরব", 24.7136, 46.6753, "Asia/Riyadh"),
        City("Jeddah", "জেদ্দা", "Saudi Arabia", "সৌদি আরব", 21.4858, 39.1925, "Asia/Riyadh"),
        City("Dubai", "দুবাই", "United Arab Emirates", "সংযুক্ত আরব আমিরাত", 25.2048, 55.2708, "Asia/Dubai"),
        City("Doha", "দোহা", "Qatar", "কাতার", 25.2854, 51.5310, "Asia/Qatar"),
        City("Kuwait City", "কুয়েত সিটি", "Kuwait", "কুয়েত", 29.3759, 47.9774, "Asia/Kuwait"),
        City("Istanbul", "ইস্তাম্বুল", "Türkiye", "তুরস্ক", 41.0082, 28.9784, "Europe/Istanbul"),
        City("Karachi", "করাচি", "Pakistan", "পাকিস্তান", 24.8607, 67.0011, "Asia/Karachi"),
        City("Lahore", "লাহোর", "Pakistan", "পাকিস্তান", 31.5204, 74.3587, "Asia/Karachi"),
        City("Delhi", "দিল্লি", "India", "ভারত", 28.6139, 77.2090, "Asia/Kolkata"),
        City("Kolkata", "কলকাতা", "India", "ভারত", 22.5726, 88.3639, "Asia/Kolkata"),
        City("Kuala Lumpur", "কুয়ালালামপুর", "Malaysia", "মালয়েশিয়া", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        City("Singapore", "সিঙ্গাপুর", "Singapore", "সিঙ্গাপুর", 1.3521, 103.8198, "Asia/Singapore"),
        City("Jakarta", "জাকার্তা", "Indonesia", "ইন্দোনেশিয়া", -6.2088, 106.8456, "Asia/Jakarta"),
        City("Cairo", "কায়রো", "Egypt", "মিসর", 30.0444, 31.2357, "Africa/Cairo"),
        City("London", "লন্ডন", "United Kingdom", "যুক্তরাজ্য", 51.5074, -0.1278, "Europe/London"),
        City("Paris", "প্যারিস", "France", "ফ্রান্স", 48.8566, 2.3522, "Europe/Paris"),
        City("Berlin", "বার্লিন", "Germany", "জার্মানি", 52.5200, 13.4050, "Europe/Berlin"),
        City("New York", "নিউ ইয়র্ক", "United States", "যুক্তরাষ্ট্র", 40.7128, -74.0060, "America/New_York"),
        City("Chicago", "শিকাগো", "United States", "যুক্তরাষ্ট্র", 41.8781, -87.6298, "America/Chicago"),
        City("Toronto", "টরন্টো", "Canada", "কানাডা", 43.6532, -79.3832, "America/Toronto"),
        City("Sydney", "সিডনি", "Australia", "অস্ট্রেলিয়া", -33.8688, 151.2093, "Australia/Sydney"),
        City("Tokyo", "টোকিও", "Japan", "জাপান", 35.6762, 139.6503, "Asia/Tokyo"),
        City("Oslo", "অসলো", "Norway", "নরওয়ে", 59.9139, 10.7522, "Europe/Oslo"),
        City("Reykjavík", "রেইকিয়াভিক", "Iceland", "আইসল্যান্ড", 64.1466, -21.9426, "Atlantic/Reykjavik"),
    )

    val all: List<City> = bangladesh + international

    fun search(query: String, bangla: Boolean): List<City> {
        if (query.isBlank()) return if (bangla) bangladesh else all.take(12)
        val q = query.trim().lowercase()
        return all.filter { city ->
            city.nameEn.lowercase().contains(q) ||
                city.nameBn.contains(query.trim()) ||
                city.countryEn.lowercase().contains(q) ||
                city.countryBn.contains(query.trim())
        }
    }

    fun nearest(latitude: Double, longitude: Double): City = all.minByOrNull { city ->
        val dLat = city.latitude - latitude
        val dLon = city.longitude - longitude
        dLat * dLat + dLon * dLon
    } ?: bangladesh.first()
}
