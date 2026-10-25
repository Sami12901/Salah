package com.salahmate.app.feature.learning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salahmate.app.core.design.arabicStyle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.content.LearningContent

private enum class LearningTab(val id: String) { NAMES("names"), KALIMA("kalima"), WUDU("wudu") }

/** Islamic learning: Asma ul Husna, the six Kalimas and a step-by-step Wudu guide. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    var tab by remember { mutableStateOf(LearningTab.NAMES) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "শিক্ষা", "Learning")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = tab == LearningTab.NAMES, onClick = { tab = LearningTab.NAMES }, label = { Text(t(bn, "আসমাউল হুসনা", "Divine names")) })
                FilterChip(selected = tab == LearningTab.KALIMA, onClick = { tab = LearningTab.KALIMA }, label = { Text(t(bn, "কালিমা", "Kalima")) })
                FilterChip(selected = tab == LearningTab.WUDU, onClick = { tab = LearningTab.WUDU }, label = { Text(t(bn, "ওজু", "Wudu")) })
            }

            when (tab) {
                LearningTab.NAMES -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        InfoBanner(
                            text = t(
                                bn,
                                "আল্লাহর ৯৯টি নাম আরবি, উচ্চারণ ও অর্থসহ দেওয়া হয়েছে। এগুলো মুখস্থ করার তালিকা, কোনো বিশেষ ফজিলতের দাবি অ্যাপ করে না।",
                                "The 99 names are listed in Arabic with transliteration and meaning. This is a memorisation aid; the app makes no claim about specific rewards."
                            ),
                            icon = Icons.Outlined.Info
                        )
                    }
                    items(LearningContent.divineNames, key = { it.number }) { name ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${name.number}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.size(28.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name.transliteration, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (bn) name.bengali else name.transliteration,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    name.arabic,
                                    style = arabicStyle(sizeSp = 22f),
                
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                LearningTab.KALIMA -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(LearningContent.kalimas, key = { it.number }) { kalima ->
                        AppCard {
                            Text(
                                "${kalima.number}. " + (if (bn) kalima.titleBn else kalima.titleEn),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                kalima.arabic,
                                style = arabicStyle(sizeSp = 24f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(kalima.transliteration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text(kalima.bengali, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                LearningTab.WUDU -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(LearningContent.wuduSteps) { step ->
                        AppCard {
                            Text(if (bn) step.titleBn else step.titleEn, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(if (bn) step.bodyBn else step.bodyEn, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            LearningContent.wuduReference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
