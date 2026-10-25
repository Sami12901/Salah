package com.salahmate.app.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.notify.NotificationPermission
import com.salahmate.app.core.prefs.NotificationSoundMode
import com.salahmate.app.domain.prayer.PrayerKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setNotificationsEnabled(true) else viewModel.refreshStatus()
    }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "রিমাইন্ডার", "Prayer reminders")) },
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
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t(bn, "নামাজের রিমাইন্ডার", "Prayer reminders"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                t(bn, "প্রতিটি নামাজের জন্য আলাদাভাবে চালু/বন্ধ করুন", "Enable or disable each prayer individually"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationPermission.isGranted(context)
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(enabled)
                                }
                            },
                        )
                    }
                    if (!ui.notificationsGranted) {
                        Spacer(Modifier.height(8.dp))
                        InfoBanner(
                            text = t(
                                bn,
                                "অনুমতি ছাড়া অ্যান্ড্রয়েড নোটিফিকেশন দেখাবে না। অনুমতি দিন, নয়তো রিমাইন্ডার কাজ করবে না।",
                                "Android will not show notifications without permission. Grant it, or reminders cannot work.",
                            ),
                            icon = Icons.Outlined.Info,
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text(t(bn, "অনুমতি দিন", "Grant permission"))
                            }
                        }
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "কোন নামাজের জন্য", "Which prayers"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    PrayerKey.obligatory.forEach { prayer ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (bn) prayer.labelBn else prayer.labelEn,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Switch(
                                checked = settings.isPrayerNotificationEnabled(prayer.id),
                                onCheckedChange = { viewModel.setPrayerNotification(prayer.id, it) },
                                enabled = settings.notificationsEnabled,
                            )
                        }
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "শব্দ", "Sound"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NotificationSoundMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.soundMode == mode,
                                onClick = { viewModel.setSoundMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            NotificationSoundMode.SILENT -> t(bn, "নীরব", "Silent")
                                            NotificationSoundMode.SYSTEM_SOUND -> t(bn, "সিস্টেম সাউন্ড", "System sound")
                                            NotificationSoundMode.ADHAN_AUDIO -> t(bn, "আজান চ্যানেল", "Adhan channel")
                                        },
                                    )
                                },
                            )
                        }
                    }
                    if (settings.soundMode == NotificationSoundMode.ADHAN_AUDIO) {
                        Spacer(Modifier.height(8.dp))
                        InfoBanner(
                            text = t(
                                bn,
                                "এই সংস্করণে আজানের অডিও ফাইল অন্তর্ভুক্ত নেই (অনুমতিযুক্ত রেকর্ডিং পাওয়া যায়নি), তাই উচ্চ-গুরুত্বের সিস্টেম সাউন্ড ব্যবহার হয়।",
                                "This build does not bundle an Adhan recording (no recording with verified redistribution rights was available), so the high-importance system sound is used instead.",
                            ),
                            icon = Icons.Outlined.Info,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t(bn, "কম্পন", "Vibration"), modifier = Modifier.weight(1f))
                        Switch(checked = settings.vibrate, onCheckedChange = viewModel::setVibrate)
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "আগাম রিমাইন্ডার", "Advance reminder"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 5, 10, 15, 30).forEach { minutes ->
                            FilterChip(
                                selected = settings.advanceMinutes == minutes,
                                onClick = { viewModel.setAdvanceMinutes(minutes) },
                                label = { Text(if (minutes == 0) t(bn, "সময়ে", "On time") else "$minutes min") },
                            )
                        }
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "যাচাই", "Verify"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(
                            bn,
                            "সঠিক অ্যালার্ম: ",
                            "Exact alarms: ",
                        ) + if (ui.exactAlarmsAvailable) t(bn, "অনুমোদিত", "allowed")
                        else t(bn, "অনুমোদিত নয় (দেরি হতে পারে)", "not allowed (may be late)"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ui.scheduleReport?.let { report ->
                        Text(
                            t(bn, "নির্ধারিত রিমাইন্ডার: ", "Scheduled reminders: ") + "${report.scheduledCount}/${report.requestedCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::sendTestNotification, modifier = Modifier.weight(1f)) {
                            Text(t(bn, "পরীক্ষা করুন", "Send test"))
                        }
                        OutlinedButton(onClick = viewModel::rescheduleNow, modifier = Modifier.weight(1f)) {
                            Text(t(bn, "পুনঃনির্ধারণ", "Reschedule"))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t(
                            bn,
                            "ব্যাটারি অপ্টিমাইজেশন বা নির্মাতার কাস্টম ব্যাটারি সেভার রিমাইন্ডার বিলম্বিত করতে পারে। ফোনের সেটিংসে SalahMate-কে সীমাবদ্ধতা থেকে মুক্ত রাখলে সবচেয়ে ভালো ফল পাবেন।",
                            "Battery optimisation and manufacturer battery savers can delay reminders. Exempting SalahMate from restrictions gives the most reliable results.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
