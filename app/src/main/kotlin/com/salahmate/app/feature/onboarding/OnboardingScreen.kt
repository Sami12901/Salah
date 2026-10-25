package com.salahmate.app.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.AppLanguage
import com.salahmate.app.core.prefs.AsrMadhab
import com.salahmate.app.core.prefs.CalculationMethodId

/**
 * Three-step first-run setup. It exists so a new user never sees a Dhaka-based schedule without
 * knowing where those numbers came from: location, calculation method and the reminder opt-in are
 * all chosen explicitly, and each step can be changed later in Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onFinished: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bn = ui.language == AppLanguage.BENGALI
    val context = LocalContext.current

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.setNotifications(granted)
    }

    // Location is optional and only requested when the user taps "use my location".
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.useDeviceLocation()
    }

    fun requestLocation() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.useDeviceLocation()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setNotifications(true)
        }
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    LinearProgressIndicator(
                        progress = { (ui.step + 1).toFloat() / ui.totalSteps },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (ui.canGoBack) {
                            OutlinedButton(onClick = viewModel::back, modifier = Modifier.weight(1f)) {
                                Text(t(bn, "পিছনে", "Back"))
                            }
                        }
                        Button(
                            onClick = {
                                if (ui.isLastStep) viewModel.finish(onFinished) else viewModel.next()
                            },
                            enabled = !ui.finishing,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (ui.isLastStep) t(bn, "শুরু করুন", "Start using Salah")
                                else t(bn, "পরবর্তী", "Continue"),
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewModel.finish(onFinished) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(t(bn, "ডিফল্ট সেটিংসে এগিয়ে যান", "Skip and use defaults"))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("Salah", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            t(bn, "ধাপ ${ui.step + 1} / ${ui.totalSteps}", "Step ${ui.step + 1} of ${ui.totalSteps}"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            when (ui.step) {
                0 -> {
                    item {
                        AppCard {
                            Text(t(bn, "ভাষা", "Language"), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = ui.language == AppLanguage.BENGALI,
                                    onClick = { viewModel.setLanguage(AppLanguage.BENGALI) },
                                    label = { Text("বাংলা") },
                                )
                                FilterChip(
                                    selected = ui.language == AppLanguage.ENGLISH,
                                    onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                                    label = { Text("English") },
                                )
                                FilterChip(
                                    selected = ui.language == AppLanguage.ARABIC,
                                    onClick = { viewModel.setLanguage(AppLanguage.ARABIC) },
                                    label = { Text("العربية") },
                                )
                            }
                        }
                    }
                    item {
                        InfoBanner(
                            text = t(
                                bn,
                                "Salah সম্পূর্ণ অফলাইনে কাজ করে: নামাজের সময় আপনার ফোনেই হিসাব হয়, কুরআনের আরবি টেক্সট অ্যাপের সাথে সংযুক্ত। কোনো অ্যাকাউন্ট নেই, বিজ্ঞাপন নেই।",
                                "Salah works fully offline: prayer times are calculated on your phone and the Quran's Arabic text ships with the app. No account, no ads.",
                            ),
                            icon = Icons.Outlined.Info,
                        )
                    }
                }

                1 -> {
                    item {
                        AppCard {
                            Text(t(bn, "আপনার অবস্থান", "Your location"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                ui.locationLabel + " · %.4f, %.4f".format(ui.latitude, ui.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { requestLocation() },
                                    enabled = !ui.locating,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (ui.locating) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.size(6.dp))
                                    Text(t(bn, "আমার লোকেশন", "Use my location"))
                                }
                            }
                            ui.locationMessage?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    item {
                        AppCard {
                            Text(t(bn, "শহর বাছুন", "Choose a city"), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = ui.cityQuery,
                                onValueChange = viewModel::setCityQuery,
                                label = { Text(t(bn, "শহর বা দেশ খুঁজুন", "Search city or country")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                viewModel.cities().take(8).forEach { city ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectCity(city) }
                                            .background(
                                                if (city.nameEn == ui.selectedCity.nameEn) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surface,
                                            )
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            if (bn) city.labelBn else city.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (city.nameEn == ui.selectedCity.nameEn) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = t(bn, "নির্বাচিত", "Selected"),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        ManualCoordinatesCard(bn = bn, onApply = viewModel::setManualCoordinates)
                    }
                }

                else -> {
                    item {
                        AppCard {
                            Text(t(bn, "গণনা পদ্ধতি", "Calculation method"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                t(bn, "আপনার এলাকার প্রচলিত পদ্ধতি বেছে নিন।", "Pick the method commonly used in your area."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CalculationMethodId.entries) { method ->
                                    FilterChip(
                                        selected = ui.method == method,
                                        onClick = { viewModel.setMethod(method) },
                                        label = { Text(if (bn) method.labelBn else method.labelEn) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(t(bn, "আসরের মাযহাব", "Asr school"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                t(bn, "গণনা পদ্ধতি ও আসরের স্কুল আলাদা সেটিং — একটি বদলালে অন্যটি বদলায় না।", "Method and Asr school are separate settings — changing one never changes the other."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AsrMadhab.entries.forEach { madhab ->
                                    FilterChip(
                                        selected = ui.madhab == madhab,
                                        onClick = { viewModel.setMadhab(madhab) },
                                        label = { Text(if (bn) madhab.labelBn else madhab.labelEn) },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t(bn, "নামাজের রিমাইন্ডার", "Prayer reminders"), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        t(bn, "প্রতিটি নামাজের জন্য আলাদা করে বন্ধ করা যাবে।", "Each prayer can be turned off individually later."),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = ui.notificationsEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) askForNotifications() else viewModel.setNotifications(false)
                                    },
                                )
                            }
                        }
                    }
                    item {
                        InfoBanner(
                            text = t(
                                bn,
                                "সময়সূচি হিসাব করা হয় — আপনার মসজিদের সময়সূচির সাথে মিলিয়ে নিন। অ্যাপ কোনো ধর্মীয় ফতোয়া দেয় না।",
                                "Times are calculated — please check them against your local mosque. The app issues no religious rulings.",
                            ),
                            icon = Icons.Outlined.Public,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualCoordinatesCard(bn: Boolean, onApply: (Double, Double) -> Unit) {
    var lat by androidx.compose.runtime.remember { mutableStateOf("") }
    var lon by androidx.compose.runtime.remember { mutableStateOf("") }
    AppCard {
        Text(t(bn, "নিজে কোঅর্ডিনেট দিন", "Enter coordinates manually"), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = lat,
                onValueChange = { lat = it.filter { c -> c.isDigit() || c == '.' || c == '-' }.take(9) },
                label = { Text(t(bn, "অক্ষাংশ", "Latitude")) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = lon,
                onValueChange = { lon = it.filter { c -> c.isDigit() || c == '.' || c == '-' }.take(10) },
                label = { Text(t(bn, "দ্রাঘিমাংশ", "Longitude")) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val la = lat.toDoubleOrNull()
                val lo = lon.toDoubleOrNull()
                if (la != null && lo != null) onApply(la, lo)
            },
            enabled = lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null,
        ) {
            Text(t(bn, "প্রয়োগ করুন", "Apply"))
        }
    }
}
