package com.salahmate.app.feature.zakat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.core.db.ZakatRecordEntity
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.NisabBasis
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.zakat.NisabBasisOption
import com.salahmate.app.domain.zakat.ZakatCalculator
import com.salahmate.app.domain.zakat.ZakatInput
import com.salahmate.app.domain.zakat.ZakatResult
import com.salahmate.app.domain.zakat.ZakatWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zakat calculator. Every number on screen comes from user input or the documented model in
 * [ZakatCalculator]; nothing is assumed to be a current market price.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(viewModel: ZakatViewModel, settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "যাকাত ক্যালকুলেটর", "Zakat calculator")) },
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
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Text(t(bn, "সম্পদ", "Assets"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    AmountField(ui.cash, t(bn, "নগদ (${ui.currency})", "Cash (${ui.currency})")) { v -> viewModel.update { it.copy(cash = v) } }
                    AmountField(ui.bank, t(bn, "ব্যাংক ব্যালেন্স", "Bank balances")) { v -> viewModel.update { it.copy(bank = v) } }
                    AmountField(ui.inventory, t(bn, "ব্যবসার মজুদ", "Business inventory")) { v -> viewModel.update { it.copy(inventory = v) } }
                    AmountField(ui.receivables, t(bn, "পাওনা টাকা", "Receivables")) { v -> viewModel.update { it.copy(receivables = v) } }
                    AmountField(ui.other, t(bn, "অন্যান্য যাকাতযোগ্য সম্পদ", "Other zakatable assets")) { v -> viewModel.update { it.copy(other = v) } }
                    AmountField(ui.debts, t(bn, "পরিশোধ্য ঋণ (বাদ যাবে)", "Due debts (deducted)")) { v -> viewModel.update { it.copy(debts = v) } }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "স্বর্ণ ও রূপা", "Gold & silver"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    AmountField(ui.goldGrams, t(bn, "স্বর্ণ (গ্রাম)", "Gold (grams)")) { v -> viewModel.update { it.copy(goldGrams = v) } }
                    AmountField(ui.goldPrice, t(bn, "স্বর্ণের দাম / গ্রাম (আপনার ইনপুট)", "Gold price / gram (your input)")) { v -> viewModel.update { it.copy(goldPrice = v) } }
                    AmountField(ui.silverGrams, t(bn, "রূপা (গ্রাম)", "Silver (grams)")) { v -> viewModel.update { it.copy(silverGrams = v) } }
                    AmountField(ui.silverPrice, t(bn, "রূপার দাম / গ্রাম (আপনার ইনপুট)", "Silver price / gram (your input)")) { v -> viewModel.update { it.copy(silverPrice = v) } }
                    Text(
                        t(bn, "দাম স্বয়ংক্রিয়ভাবে আনা হয় না — বাজারদর নিজে যাচাই করে লিখুন।", "Prices are never fetched automatically — enter today's market rate yourself."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    Text(t(bn, "নিসাবের ভিত্তি", "Nisab basis"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = ui.nisab == NisabBasisOption.GOLD,
                            onClick = { viewModel.setNisab(NisabBasisOption.GOLD) },
                            label = { Text(t(bn, "স্বর্ণ (৮৭.৪৮ গ্রাম)", "Gold (87.48 g)")) },
                        )
                        FilterChip(
                            selected = ui.nisab == NisabBasisOption.SILVER,
                            onClick = { viewModel.setNisab(NisabBasisOption.SILVER) },
                            label = { Text(t(bn, "রূপা (৬১২.৩৬ গ্রাম)", "Silver (612.36 g)")) },
                        )
                    }
                }
            }

            item {
                val result = viewModel.result()
                ZakatResultCard(result = result, bangla = bn)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::save, modifier = Modifier.weight(1f)) {
                        Text(t(bn, "হিসাব সংরক্ষণ", "Save calculation"))
                    }
                    OutlinedButton(onClick = viewModel::reset, modifier = Modifier.weight(1f)) {
                        Text(t(bn, "রিসেট", "Reset"))
                    }
                }
            }

            if (ui.saved.isNotEmpty()) {
                item {
                    AppCard {
                        Text(t(bn, "সংরক্ষিত হিসাব", "Saved calculations"), style = MaterialTheme.typography.titleMedium)
                        val formatter = remember { SimpleDateFormat("d MMM yyyy", if (bn) Locale("bn", "BD") else Locale.ENGLISH) }
                        ui.saved.take(10).forEach { record: ZakatRecordEntity ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "%.2f %s".format(record.zakatDue, record.currency),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        formatter.format(Date(record.createdAt)) + " · " + t(bn, "নিট সম্পদ ", "net wealth ") +
                                            "%.2f".format(record.netWealth),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                InfoBanner(
                    text = t(
                        bn,
                        "এটি একটি তথ্যগত সহায়ক, ফতোয়া নয়। সম্পদের ধরন অনুযায়ী আলেমদের মতভেদ থাকতে পারে — জটিল ক্ষেত্রে যোগ্য আলেম বা হিসাবরক্ষকের পরামর্শ নিন।",
                        "This is an informational aid, not a fatwa. Scholarly views differ on some asset categories — consult a qualified scholar or accountant for complex cases.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }
        }
    }
}

/**
 * Numeric field that keeps the user's raw text (so typing "12." does not jump) while reporting a
 * parsed Double upwards.
 */
@Composable
private fun AmountField(value: Double, label: String, onValueChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (value == 0.0) "" else trimNumber(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || it == '.' }
            text = filtered
            onValueChange(filtered.toDoubleOrNull() ?: 0.0)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

private fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else "%.2f".format(value)

@Composable
private fun ZakatResultCard(result: ZakatResult, bangla: Boolean) {
    AppCard {
        Text(t(bangla, "হিসাবের সারসংক্ষেপ", "Calculation summary"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        result.lines.filter { it.amount != 0.0 }.forEach { line ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    if (bangla) line.labelBn else line.labelEn,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("%.2f".format(line.amount), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(t(bangla, "মোট সম্পদ", "Total assets"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("%.2f %s".format(result.totalAssets, result.currency), style = MaterialTheme.typography.bodyMedium)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(t(bangla, "বাদ: ঋণ", "Less: debts"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("%.2f".format(result.totalLiabilities), style = MaterialTheme.typography.bodyMedium)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(t(bangla, "নিট যাকাতযোগ্য সম্পদ", "Net zakatable wealth"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("%.2f".format(result.netWealth), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            t(bangla, "নিসাব (", "Nisab (") +
                when (result.nisabBasis) {
                    NisabBasisOption.GOLD -> t(bangla, "স্বর্ণ", "gold")
                    NisabBasisOption.SILVER -> t(bangla, "রূপা", "silver")
                } + "): " + "%.2f %s".format(result.nisabApplied, result.currency),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            color = if (result.exceedsNisab) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(t(bangla, "প্রদেয় যাকাত (২.৫%)", "Zakat due (2.5%)"), style = MaterialTheme.typography.labelMedium)
                Text(
                    "%.2f %s".format(result.zakatDue, result.currency),
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (!result.exceedsNisab) {
                    Text(
                        t(bangla, "সম্পদ নিসাবের নিচে — এই মডেলে যাকাত ফরজ হয়নি।", "Wealth is below the nisab — no zakat is due under this model."),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        result.warnings.forEach { warning ->
            Spacer(Modifier.height(6.dp))
            Text(
                when (warning) {
                    ZakatWarning.MISSING_GOLD_PRICE -> t(bangla, "স্বর্ণের দাম দেওয়া হয়নি, তাই স্বর্ণের মূল্য ০ ধরা হয়েছে।", "No gold price entered, so gold is valued at zero.")
                    ZakatWarning.MISSING_SILVER_PRICE -> t(bangla, "রূপার দাম দেওয়া হয়নি, তাই রূপার মূল্য ০ ধরা হয়েছে।", "No silver price entered, so silver is valued at zero.")
                    ZakatWarning.STALE_PRICES -> t(bangla, "দাম পুরনো হতে পারে — হালনাগাদ করুন।", "Prices may be out of date — please refresh them.")
                    ZakatWarning.BELOW_NISAB -> t(bangla, "সম্পদ নিসাবের নিচে।", "Wealth is below nisab.")
                    ZakatWarning.NO_ASSETS -> t(bangla, "কোনো সম্পদ লেখা হয়নি।", "No assets entered yet.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
