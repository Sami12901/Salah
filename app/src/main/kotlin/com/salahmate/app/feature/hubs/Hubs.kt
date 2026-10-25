package com.salahmate.app.feature.hubs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salahmate.app.Routes
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.t
import com.salahmate.app.core.design.t3
import com.salahmate.app.core.prefs.UserSettings

private data class HubItem(
    val route: String,
    val icon: ImageVector,
    val titleEn: String,
    val titleBn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleBn: String,
)

/**
 * Tab hubs. Each tab lists the modules it contains with an accurate description — no fake counters
 * or placeholder numbers.
 */
@Composable
fun MoreScreen(settings: UserSettings, onNavigate: (String) -> Unit) {
    val bn = settings.isBanglaUi
    val arabic = settings.isArabicUi
    val items = listOf(
        HubItem(Routes.QIBLA, Icons.Outlined.Explore, "Qibla", "কিবলা", "القبلة", "Compass and numeric bearing", "কম্পাস ও সংখ্যাগত দিক"),
        HubItem(Routes.ZAKAT, Icons.Outlined.Calculate, "Zakat calculator", "যাকাত ক্যালকুলেটর", "حاسبة الزكاة", "Nisab, assets and a full breakdown", "নিসাব, সম্পদ ও পূর্ণ হিসাব"),
        HubItem(Routes.RAMADAN, Icons.Outlined.NightsStay, "Ramadan", "রমজান", "رمضان", "Suhoor, Iftar and the monthly calendar", "সেহরি, ইফতার ও মাসিক ক্যালেন্ডার"),
        HubItem(Routes.TIMETABLE, Icons.Outlined.Schedule, "Monthly timetable", "মাসিক সময়সূচি", "الجدول الشهري", "Every prayer time for the whole month", "পুরো মাসের নামাজের সময়সূচি"),
        HubItem(Routes.CALENDAR, Icons.Outlined.CalendarMonth, "Islamic calendar", "ইসলামিক ক্যালেন্ডার", "التقويم الهجري", "Hijri dates and events", "হিজরি তারিখ ও ইসলামিক দিবস"),
        HubItem(Routes.MOSQUE, Icons.Outlined.Mosque, "Mosque finder", "মসজিদ খুঁজুন", "البحث عن مسجد", "Open nearby mosques in a map app", "ম্যাপ অ্যাপে কাছের মসজিদ দেখুন"),
        HubItem(Routes.SETTINGS, Icons.Outlined.Settings, "Settings", "সেটিংস", "الإعدادات", "Location, calculation, notifications, data", "অবস্থান, গণনা, রিমাইন্ডার, ডেটা"),
    )
    HubScaffold(titleEn = "More", titleBn = "আরও", titleAr = "المزيد", bangla = bn, arabic = arabic) {
        items.forEach { item ->
            item {
                HubRow(item = item, bangla = bn, arabic = arabic, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
fun TrackScreen(settings: UserSettings, onNavigate: (String) -> Unit) {
    val bn = settings.isBanglaUi
    val arabic = settings.isArabicUi
    val items = listOf(
        HubItem(Routes.PRAYER_TRACKING, Icons.Outlined.History, "Prayer tracking", "নামাজ ট্র্যাকিং", "تتبع الصلوات", "Private daily checklist and history", "ব্যক্তিগত দৈনিক চেকলিস্ট ও ইতিহাস"),
        HubItem(Routes.HIFZ, Icons.Outlined.School, "Hifz tracker", "হিফজ ট্র্যাকার", "متابعة الحفظ", "Memorisation states and review schedule", "মুখস্থের অবস্থা ও পুনরাবৃত্তির সময়সূচি"),
        HubItem(Routes.TASBIH, Icons.Outlined.TouchApp, "Tasbih", "তাসবিহ", "المسبحة", "Tap counter with saved sessions", "ট্যাপ কাউন্টার ও সংরক্ষিত সেশন"),
        HubItem(Routes.DUA, Icons.Outlined.SelfImprovement, "Dua & Adhkar", "দুআ ও আযকার", "الدعاء والأذكار", "Morning/evening and daily supplications", "সকাল-সন্ধ্যা ও দৈনন্দিন দুআ"),
    )
    HubScaffold(titleEn = "Track", titleBn = "ট্র্যাক", titleAr = "التتبع", bangla = bn, arabic = arabic) {
        items.forEach { item ->
            item {
                HubRow(item = item, bangla = bn, arabic = arabic, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
fun LearnScreen(settings: UserSettings, onNavigate: (String) -> Unit) {
    val bn = settings.isBanglaUi
    val arabic = settings.isArabicUi
    val items = listOf(
        HubItem(Routes.QURAN, Icons.Outlined.MenuBook, "Quran reader", "কুরআন পাঠ", "المصحف", "114 surahs, offline Arabic text", "১১৪ সূরা, অফলাইন আরবি টেক্সট"),
        HubItem(Routes.HADITH, Icons.Outlined.AutoStories, "Hadith", "হাদিস", "الحديث", "Referenced collections with grading", "তথ্যসূত্রসহ হাদিস সংকলন"),
        HubItem(Routes.DUA, Icons.Outlined.SelfImprovement, "Dua & Adhkar", "দুআ ও আযকার", "الدعاء والأذكار", "Sourced supplications by occasion", "উৎসসহ প্রার্থনা"),
        HubItem(Routes.LEARNING, Icons.Outlined.School, "Learning", "শিক্ষা", "التعلّم", "Asma ul Husna, Kalima, Wudu guide", "আসমাউল হুসনা, কালিমা, ওজুর নিয়ম"),
    )
    HubScaffold(titleEn = "Learn", titleBn = "শিখুন", titleAr = "التعلّم", bangla = bn, arabic = arabic) {
        items.forEach { item ->
            item {
                HubRow(item = item, bangla = bn, arabic = arabic, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun HubScaffold(
    titleEn: String,
    titleBn: String,
    titleAr: String,
    bangla: Boolean,
    arabic: Boolean,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            t3(bangla, arabic, titleBn, titleEn, titleAr),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun HubRow(item: HubItem, bangla: Boolean, arabic: Boolean, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    t3(bangla, arabic, item.titleBn, item.titleEn, item.titleAr),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    t(bangla, item.subtitleBn, item.subtitleEn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
