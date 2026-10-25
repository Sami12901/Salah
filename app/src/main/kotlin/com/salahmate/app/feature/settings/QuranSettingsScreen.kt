package com.salahmate.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.data.quran.QuranEditions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "কুরআন সেটিংস", "Quran settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Text(t(bn, "আরবি লেখার আকার", "Arabic text size"), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = settings.arabicScale,
                        onValueChange = viewModel::setArabicScale,
                        valueRange = 0.8f..2.0f,
                        steps = 11,
                    )
                    Text(
                        "%.2f×".format(settings.arabicScale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t(bn, "অনুবাদ দেখান", "Show translation"), modifier = Modifier.weight(1f))
                        Switch(checked = settings.showTranslation, onCheckedChange = viewModel::setShowTranslation)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(bn, "অনুবাদ ডাউনলোড করা হয় এবং ডিভাইসে সংরক্ষিত থাকে।", "Translations are downloaded on demand and stored on this device."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                AppCard {
                    Text(t(bn, "বাংলা অনুবাদ", "Bengali translation"), style = MaterialTheme.typography.titleMedium)
                    QuranEditions.BENGALI.forEach { edition ->
                        SelectionRow(
                            label = "${edition.label} (${edition.id})",
                            selected = settings.translationEdition == edition.id,
                            onClick = { viewModel.setTranslationEdition(edition.id) },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(t(bn, "ইংরেজি অনুবাদ", "English translation"), style = MaterialTheme.typography.titleMedium)
                    QuranEditions.ENGLISH.forEach { edition ->
                        SelectionRow(
                            label = "${edition.label} (${edition.id})",
                            selected = settings.translationEdition == edition.id,
                            onClick = { viewModel.setTranslationEdition(edition.id) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoBanner(
                        text = t(
                            bn,
                            "অনুবাদ মানে অনুবাদ — মূল আরবি কুরআন নয়। প্রতিটি স্ক্রিনে অনুবাদকের নাম দেখানো হয়।",
                            "A translation is a rendering of meaning, not the Qur'an itself. The translator is credited on every screen.",
                        ),
                        icon = Icons.Outlined.Info,
                    )
                }
            }
            item {
                AppCard {
                    Text(t(bn, "ক্বারী (অডিও)", "Reciter (audio)"), style = MaterialTheme.typography.titleMedium)
                    QuranEditions.RECITERS.forEach { reciter ->
                        SelectionRow(
                            label = reciter.label,
                            selected = settings.reciterEdition == reciter.id,
                            onClick = { viewModel.setReciter(reciter.id) },
                        )
                    }
                }
            }
            item {
                AppCard {
                    Text(t(bn, "দৈনিক আয়াত লক্ষ্য", "Daily ayah goal"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 20, 50, 100).forEach { goal ->
                            androidx.compose.material3.FilterChip(
                                selected = settings.dailyAyahGoal == goal,
                                onClick = { viewModel.setDailyAyahGoal(goal) },
                                label = { Text("$goal") },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
