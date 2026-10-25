package com.salahmate.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.BuildConfig
import com.salahmate.app.Routes
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.location.Cities
import com.salahmate.app.core.location.City
import com.salahmate.app.core.prefs.AppLanguage
import com.salahmate.app.core.prefs.AsrMadhab
import com.salahmate.app.core.prefs.CalculationMethodId
import com.salahmate.app.core.prefs.HighLatitudeRuleId
import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.core.prefs.PrayerProviderMode
import com.salahmate.app.core.prefs.ThemeMode
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.prayer.PrayerKey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    val snackbar = remember { SnackbarHostState() }
    var showCityPicker by remember { mutableStateOf(false) }
    var showManualCoordinates by remember { mutableStateOf(false) }
    var showMethodPicker by remember { mutableStateOf(false) }
    var showMadhabPicker by remember { mutableStateOf(false) }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "সেটিংস", "Settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Text(t(bn, "ভাষা ও চেহারা", "Language & appearance"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.language == AppLanguage.BENGALI,
                            onClick = { viewModel.setLanguage(AppLanguage.BENGALI) },
                            label = { Text("বাংলা") },
                        )
                        FilterChip(
                            selected = settings.language == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") },
                        )
                        FilterChip(
                            selected = settings.language == AppLanguage.ARABIC,
                            onClick = { viewModel.setLanguage(AppLanguage.ARABIC) },
                            label = { Text("العربية") },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t(
                            bn,
                            "আরবি নির্বাচন করলে লেআউট ডান-থেকে-বাম হবে; নেভিগেশন ও মডিউলের নাম আরবিতে, বাকি স্ক্রিন ইংরেজিতে দেখাবে। কুরআন, দুআ ও হাদিসের আরবি টেক্সট সবসময় আরবিতেই থাকে।",
                            "Choosing Arabic flips the layout to right-to-left and translates navigation and module titles; deeper screens fall back to English. Quran, dua and hadith Arabic text is always shown in Arabic.",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setTheme(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> t(bn, "সিস্টেম", "System")
                                            ThemeMode.LIGHT -> t(bn, "লাইট", "Light")
                                            ThemeMode.DARK -> t(bn, "ডার্ক", "Dark")
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t(bn, "অবস্থান", "Location"), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(settings.locationLabel, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "%.4f, %.4f · %s".format(settings.latitude, settings.longitude, settings.timeZoneId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showCityPicker = true }, modifier = Modifier.weight(1f)) {
                            Text(t(bn, "শহর বাছুন", "Choose city"))
                        }
                        OutlinedButton(onClick = { showManualCoordinates = true }, modifier = Modifier.weight(1f)) {
                            Text(t(bn, "স্থানাঙ্ক", "Coordinates"))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.useDeviceLocation() },
                        enabled = !ui.locationBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (ui.locationBusy) t(bn, "খোঁজা হচ্ছে…", "Locating…")
                            else t(bn, "ডিভাইসের অবস্থান ব্যবহার করুন (ঐচ্ছিক)", "Use device location (optional)"),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(
                            bn,
                            "জিপিএস ছাড়াও অ্যাপ পুরোপুরি কাজ করে — শহর বাছাই বা স্থানাঙ্ক লিখলেই হবে। অবস্থান কোথাও সংরক্ষণ করা হয় না।",
                            "The app works fully without GPS — picking a city or entering coordinates is enough. Your location is never stored or sent anywhere.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    Text(t(bn, "নামাজের সময় গণনা", "Prayer time calculation"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    SettingRow(
                        title = t(bn, "পদ্ধতি", "Method"),
                        value = if (bn) settings.method.labelBn else settings.method.labelEn,
                        onClick = { showMethodPicker = true },
                    )
                    SettingRow(
                        title = t(bn, "আসরের মাঝহাব", "Asr madhab"),
                        value = if (bn) settings.madhab.labelBn else settings.madhab.labelEn,
                        onClick = { showMadhabPicker = true },
                    )
                    SettingRow(
                        title = t(bn, "উৎস", "Source"),
                        value = when (settings.providerMode) {
                            PrayerProviderMode.LOCAL -> t(bn, "ডিভাইসে গণনা (অফলাইন)", "On-device (offline)")
                            PrayerProviderMode.ALADHAN_ONLINE -> t(bn, "আল-আধান এপিআই", "AlAdhan API")
                            PrayerProviderMode.ALADHAN_WITH_LOCAL_FALLBACK -> t(bn, "এপিআই, ব্যর্থ হলে ডিভাইসে", "API, on-device fallback")
                        },
                        onClick = {
                            val next = when (settings.providerMode) {
                                PrayerProviderMode.LOCAL -> PrayerProviderMode.ALADHAN_WITH_LOCAL_FALLBACK
                                PrayerProviderMode.ALADHAN_WITH_LOCAL_FALLBACK -> PrayerProviderMode.ALADHAN_ONLINE
                                PrayerProviderMode.ALADHAN_ONLINE -> PrayerProviderMode.LOCAL
                            }
                            viewModel.setProviderMode(next)
                        },
                    )
                    SettingRow(
                        title = t(bn, "উচ্চ অক্ষাংশের নিয়ম", "High-latitude rule"),
                        value = when (settings.highLatitudeRule) {
                            HighLatitudeRuleId.MIDDLE_OF_THE_NIGHT -> t(bn, "রাতের মধ্যভাগ", "Middle of the night")
                            HighLatitudeRuleId.SEVENTH_OF_THE_NIGHT -> t(bn, "রাতের এক-সপ্তমাংশ", "Seventh of the night")
                            HighLatitudeRuleId.TWILIGHT_ANGLE -> t(bn, "গোধূলি কোণ", "Twilight angle")
                        },
                        onClick = {
                            val next = when (settings.highLatitudeRule) {
                                HighLatitudeRuleId.MIDDLE_OF_THE_NIGHT -> HighLatitudeRuleId.SEVENTH_OF_THE_NIGHT
                                HighLatitudeRuleId.SEVENTH_OF_THE_NIGHT -> HighLatitudeRuleId.TWILIGHT_ANGLE
                                HighLatitudeRuleId.TWILIGHT_ANGLE -> HighLatitudeRuleId.MIDDLE_OF_THE_NIGHT
                            }
                            viewModel.setHighLatitudeRule(next)
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    InfoBanner(
                        text = t(
                            bn,
                            "মনে রাখবেন: এগুলো গণনাকৃত সময়। স্থানীয় মসজিদ বা সরকারি সময়সূচির সাথে কিছুটা ভিন্ন হতে পারে — মিনিট সমন্বয় দিয়ে মিলিয়ে নিতে পারেন।",
                            "Remember: these are calculated times. They can differ slightly from your local mosque or government timetable — use the minute adjustments to align them.",
                        ),
                        icon = Icons.Outlined.Info,
                    )
                }
            }

            item { AdjustmentCard(settings = settings, bangla = bn, onApply = viewModel::setAdjustments) }

            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t(bn, "রিমাইন্ডার", "Reminders"), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (ui.notificationsGranted) t(bn, "অনুমতি দেওয়া হয়েছে", "Permission granted")
                        else t(bn, "অনুমতি দেওয়া হয়নি", "Permission not granted"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ui.notificationsGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    if (!ui.exactAlarmsAvailable) {
                        Spacer(Modifier.height(6.dp))
                        InfoBanner(
                            text = t(
                                bn,
                                "সঠিক সময়ে অ্যালার্মের অনুমতি নেই। রিমাইন্ডার কিছুটা দেরিতে আসতে পারে — ফোনের সেটিংসে SalahMate-কে অ্যালার্ম ও রিমাইন্ডার অনুমতি দিন।",
                                "Exact alarms are not allowed for this app. Reminders may arrive a few minutes late — allow alarms and reminders for SalahMate in system settings.",
                            ),
                            icon = Icons.Outlined.Info,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onNavigate(Routes.NOTIFICATION_SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                        Text(t(bn, "রিমাইন্ডার সেটিংস", "Reminder settings"))
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "কুরআন", "Quran"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t(bn, "ফন্ট সাইজ", "Arabic text size") + ": " + "%.2f×".format(settings.arabicScale),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = settings.arabicScale,
                        onValueChange = viewModel::setArabicScale,
                        valueRange = 0.8f..2.0f,
                        steps = 11,
                    )
                    SettingRow(
                        title = t(bn, "অনুবাদ", "Translation"),
                        value = settings.translationEdition,
                        onClick = { onNavigate(Routes.QURAN_SETTINGS) },
                    )
                    SettingRow(
                        title = t(bn, "দৈনিক আয়াত লক্ষ্য", "Daily ayah goal"),
                        value = settings.dailyAyahGoal.toString(),
                        onClick = { onNavigate(Routes.QURAN_SETTINGS) },
                    )
                }
            }

            item {
                AppCard {
                    Text(t(bn, "হিজরি তারিখ", "Hijri date"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(bn, "সমন্বয়: ", "Adjustment: ") + "${settings.hijriOffsetDays} " + t(bn, "দিন", "day(s)"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(-2, -1, 0, 1, 2).forEach { offset ->
                            FilterChip(
                                selected = settings.hijriOffsetDays == offset,
                                onClick = { viewModel.setHijriOffset(offset) },
                                label = { Text(if (offset > 0) "+$offset" else "$offset") },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(
                            bn,
                            "হিজরি তারিখ গণনা করে নির্ধারণ করা হয়; স্থানীয় চাঁদ দেখার ঘোষণায় এক দিন ভিন্ন হতে পারে।",
                            "The Hijri date here is calculated arithmetically and may differ by a day from local moon-sighting announcements.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    Text(t(bn, "ডেটা ও গোপনীয়তা", "Data & privacy"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(bn, "ব্যবহৃত স্টোরেজ", "Storage used") + ": " +
                            "%.1f MB".format((ui.storageBytes + ui.hadithStorageBytes) / 1024.0 / 1024.0),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onNavigate(Routes.DATA_SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                        Text(t(bn, "ব্যাকআপ ও পুনরুদ্ধার", "Backup & restore"))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onNavigate(Routes.ABOUT) }, modifier = Modifier.fillMaxWidth()) {
                        Text(t(bn, "অ্যাপ সম্পর্কে ও গোপনীয়তা নীতি", "About & privacy policy"))
                    }
                }
            }

            item {
                Text(
                    "SalahMate ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            bangla = bn,
            onDismiss = { showCityPicker = false },
            onPick = { city ->
                viewModel.selectCity(city)
                showCityPicker = false
            },
        )
    }
    if (showManualCoordinates) {
        ManualCoordinatesDialog(
            bangla = bn,
            onDismiss = { showManualCoordinates = false },
            onConfirm = { lat, lon, label ->
                viewModel.setManualCoordinates(lat, lon, label)
                showManualCoordinates = false
            },
        )
    }
    if (showMethodPicker) {
        ChoiceDialog(
            title = t(bn, "গণনা পদ্ধতি", "Calculation method"),
            options = CalculationMethodId.entries.map { it to (if (bn) it.labelBn else it.labelEn) },
            selected = settings.method,
            onDismiss = { showMethodPicker = false },
            onSelect = {
                viewModel.setMethod(it)
                showMethodPicker = false
            },
        )
    }
    if (showMadhabPicker) {
        ChoiceDialog(
            title = t(bn, "আসরের মাঝহাব", "Asr madhab"),
            options = AsrMadhab.entries.map { it to (if (bn) it.labelBn else it.labelEn) },
            selected = settings.madhab,
            onDismiss = { showMadhabPicker = false },
            onSelect = {
                viewModel.setMadhab(it)
                showMadhabPicker = false
            },
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AdjustmentCard(settings: UserSettings, bangla: Boolean, onApply: (Int, Int, Int, Int, Int, Int) -> Unit) {
    var values by remember(settings) {
        mutableStateOf(
            listOf(
                settings.adjustmentFajr, settings.adjustmentSunrise, settings.adjustmentDhuhr,
                settings.adjustmentAsr, settings.adjustmentMaghrib, settings.adjustmentIsha,
            ),
        )
    }
    AppCard {
        Text(t(bangla, "মিনিট সমন্বয়", "Minute adjustments"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            t(
                bangla,
                "স্থানীয় সময়সূচির সাথে মিলাতে প্রতিটি নামাজ আলাদাভাবে সমন্বয় করুন (−10 থেকে +10 মিনিট)।",
                "Align with your local timetable by shifting each prayer individually (−10 to +10 minutes).",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        PrayerKey.entries.forEachIndexed { index, key ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (bangla) key.labelBn else key.labelEn, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(
                    onClick = { values = values.toMutableList().also { it[index] = (it[index] - 1).coerceAtLeast(-10) } },
                    modifier = Modifier.semantics { contentDescription = "Decrease ${key.id}" },
                ) { Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp)) }
                Text("${values[index]}", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.bodyMedium)
                IconButton(
                    onClick = { values = values.toMutableList().also { it[index] = (it[index] + 1).coerceAtMost(10) } },
                    modifier = Modifier.semantics { contentDescription = "Increase ${key.id}" },
                ) { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onApply(values[0], values[1], values[2], values[3], values[4], values[5]) },
                modifier = Modifier.weight(1f),
            ) { Text(t(bangla, "প্রয়োগ করুন", "Apply")) }
            OutlinedButton(
                onClick = { values = List(6) { 0 } },
                modifier = Modifier.weight(1f),
            ) { Text(t(bangla, "রিসেট", "Reset")) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPickerDialog(bangla: Boolean, onDismiss: () -> Unit, onPick: (City) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, bangla) { Cities.search(query, bangla).take(120) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(bangla, "শহর বাছুন", "Choose a city")) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(t(bangla, "খুঁজুন", "Search")) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(results) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(city) }
                                .padding(vertical = 10.dp),
                        ) {
                            Column {
                                Text(if (bangla) city.nameBn else city.nameEn, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (bangla) city.countryBn else city.countryEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t(bangla, "বাতিল", "Cancel")) }
        },
    )
}

@Composable
private fun ManualCoordinatesDialog(
    bangla: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, String) -> Unit,
) {
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val latValue = lat.toDoubleOrNull()
    val lonValue = lon.toDoubleOrNull()
    val valid = latValue != null && lonValue != null && latValue in -90.0..90.0 && lonValue in -180.0..180.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(bangla, "স্থানাঙ্ক লিখুন", "Enter coordinates")) },
        text = {
            Column {
                OutlinedTextField(
                    value = lat,
                    onValueChange = { lat = it },
                    label = { Text(t(bangla, "অক্ষাংশ (−90…90)", "Latitude (−90…90)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = lon,
                    onValueChange = { lon = it },
                    label = { Text(t(bangla, "দ্রাঘিমাংশ (−180…180)", "Longitude (−180…180)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(t(bangla, "নাম (ঐচ্ছিক)", "Label (optional)")) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(latValue!!, lonValue!!, label) },
            ) { Text(t(bangla, "সংরক্ষণ", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t(bangla, "বাতিল", "Cancel")) } },
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
