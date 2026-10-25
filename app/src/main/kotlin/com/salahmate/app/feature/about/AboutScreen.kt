package com.salahmate.app.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salahmate.app.BuildConfig
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings

/**
 * About / privacy summary.
 *
 * This text is written to match what the app actually does: no account, no analytics, no ads, and
 * network access limited to the specific content APIs documented in docs/API_SOURCES.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "অ্যাপ সম্পর্কে", "About Salah")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Text("Salah", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        t(bn, "সংস্করণ ", "Version ") + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t(
                            bn,
                            "একটি অফলাইন-প্রথম নামাজ, কুরআন ও দৈনন্দিন ইবাদত সহায়ক। কোনো অ্যাকাউন্ট নেই, বিজ্ঞাপন নেই, বিশ্লেষণ (analytics) নেই।",
                            "An offline-first companion for prayer times, the Quran and daily worship. No account, no ads, no analytics.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                AppCard {
                    Text(t(bn, "গোপনীয়তা", "Privacy"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        t(
                            bn,
                            "আপনার নামাজের সময়সূচি, বুকমার্ক, হিফজ রেকর্ড, যাকাতের হিসাব, তাসবিহ ও ট্র্যাকিং — সব এই ডিভাইসে সংরক্ষিত।",
                            "Prayer schedule data, bookmarks, hifz records, zakat calculations, tasbih and tracking are stored on this device.",
                        ),
                        t(
                            bn,
                            "অবস্থান শুধু সময়সূচি হিসাবের জন্য ব্যবহৃত হয়; ব্যবহারকারী না চাইলে GPS চালু হয় না এবং কোনো সার্ভারে পাঠানো হয় না।",
                            "Location is used only to calculate prayer times; GPS is requested only when you ask, and coordinates are never uploaded.",
                        ),
                        t(
                            bn,
                            "ইন্টারনেট ব্যবহার হয় শুধু: কুরআনের অনুবাদ/তেলাওয়াতের অডিও ডাউনলোড, এবং আপনি বেছে নিলে আল-আধান সময়সূচি।",
                            "Network access is limited to Quran translation/audio downloads you request and, if you opt in, the AlAdhan prayer-time provider.",
                        ),
                        t(
                            bn,
                            "কোনো ট্র্যাকিং SDK, বিজ্ঞাপন SDK বা তৃতীয় পক্ষের বিশ্লেষণ নেই। ব্যাকআপ ফাইল আপনি নিজে তৈরি করেন এবং আপনি নিজের পাসওয়ার্ড দিয়ে সুরক্ষিত থাকে।",
                            "No tracking, advertising or third-party analytics SDK is included. Backups are created by you, encrypted with a password you choose.",
                        ),
                    ).forEach { line ->
                        Text("•  $line", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            item {
                InfoBanner(
                    text = t(
                        bn,
                        "সময়সূচি আনুমানিক হিসাব — আপনার স্থানীয় মসজিদ বা সরকারি সময়সূচির সাথে মিলিয়ে দেখুন। অ্যাপ কোনো ধর্মীয় ফতোয়া বা চাঁদ দেখার ঘোষণা দেয় না।",
                        "Prayer times are calculations — please verify against your local mosque or official timetable. The app does not issue religious rulings or moon-sighting announcements.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }

            item {
                AppCard {
                    Text(t(bn, "তথ্যসূত্র ও লাইসেন্স", "Sources & licences"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(
                            bn,
                            "কুরআনের আরবি টেক্সট ও অনুবাদ: AlQuran.cloud। হাদিস: ফাওয়াজ আহমেদের হাদিস-এপিআই (Unlicense), মূল উৎস sunnah.com। সময়সূচি গণনা: Adhan (বাটুল অ্যাপস)। স্থানীয় সংকলিত দুআ ও শিক্ষামূলক বিষয়বস্তু উৎসসহ দেওয়া আছে। বিস্তারিত docs/CONTENT_LICENSES.md-এ।",
                            "Quran Arabic text and translations: AlQuran.cloud. Hadith: Fawaz Ahmed's hadith-api (Unlicense), originally from sunnah.com. Prayer calculations: Adhan by Batoul Apps. Bundled dua and learning content carry their sources inline. Full details in docs/CONTENT_LICENSES.md.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                InfoBanner(
                    text = t(
                        bn,
                        "এই অ্যাপটি কোনো প্রাতিষ্ঠানিক অনুমোদনপ্রাপ্ত অ্যাপ নয় এবং কোনো বিশেষ সংস্থার সাথে সম্পৃক্ত নয়।",
                        "This app is not endorsed by or affiliated with any institution or authority.",
                    ),
                    icon = Icons.Outlined.Lock,
                )
            }
        }
    }
}
