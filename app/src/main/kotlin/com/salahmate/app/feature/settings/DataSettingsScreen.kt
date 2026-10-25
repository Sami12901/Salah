package com.salahmate.app.feature.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t

/**
 * Backup/restore. Exports contain personal religious records, so a password is offered and
 * recommended; the UI states plainly whether a file was encrypted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    val snackbar = remember { SnackbarHostState() }

    var pendingExport by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmClearQuran by remember { mutableStateOf(false) }
    var confirmClearHadith by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> if (uri != null) viewModel.exportData(uri, password.ifBlank { null }) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importData(uri, password.ifBlank { null }) }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "ব্যাকআপ ও পুনরুদ্ধার", "Backup & restore")) },
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
                InfoBanner(
                    text = t(
                        bn,
                        "ব্যাকআপে আপনার বুকমার্ক, হিফজ অগ্রগতি, নামাজ ট্র্যাকিং, তাসবিহ ও ব্যক্তিগত নোট থাকে — এগুলো সংবেদনশীল। পাসওয়ার্ড দিয়ে এনক্রিপ্ট করাই সুপারিশ করা হয়।",
                        "A backup contains your bookmarks, hifz progress, prayer tracking, tasbih sessions and personal notes — that is sensitive. Encrypting it with a password is recommended.",
                    ),
                    icon = Icons.Outlined.Lock,
                )
            }
            item {
                AppCard {
                    Text(t(bn, "রপ্তানি", "Export"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(t(bn, "পাসওয়ার্ড (সুপারিশকৃত)", "Password (recommended)")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t(
                            bn,
                            "পাসওয়ার্ড খালি রাখলে ফাইলটি এনক্রিপ্ট হবে না।",
                            "Leave the password empty to write an unencrypted file.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = !ui.busy,
                        onClick = {
                            pendingExport = true
                            exportLauncher.launch("salahmate-backup.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(t(bn, "ব্যাকআপ ফাইল তৈরি করুন", "Create backup file")) }
                }
            }
            item {
                AppCard {
                    Text(t(bn, "পুনরুদ্ধার", "Restore"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(bn, "পুনরুদ্ধার সংযোজনমূলক — বর্তমান তথ্য মুছে যায় না।", "Restore is additive — existing data is not deleted."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        enabled = !ui.busy,
                        onClick = {
                            pendingImport = true
                            importLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(t(bn, "ব্যাকআপ ফাইল থেকে পুনরুদ্ধার", "Restore from file")) }
                }
            }
            item {
                AppCard {
                    Text(t(bn, "স্টোরেজ", "Storage"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(bn, "কুরআন অনুবাদ ও অডিও: ", "Quran translations & audio: ") +
                            "%.2f MB".format(ui.storageBytes / 1024.0 / 1024.0),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        t(bn, "হাদিস ডাউনলোড: ", "Hadith downloads: ") +
                            "%.2f MB".format(ui.hadithStorageBytes / 1024.0 / 1024.0),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { confirmClearQuran = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.height(16.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(t(bn, "কুরআন ক্যাশ", "Quran cache"))
                        }
                        OutlinedButton(onClick = { confirmClearHadith = true }, modifier = Modifier.weight(1f)) {
                            Text(t(bn, "হাদিস ডাউনলোড", "Hadith files"))
                        }
                    }
                }
            }
            item {
                InfoBanner(
                    text = t(
                        bn,
                        "SalahMate কোনো তথ্য সার্ভারে পাঠায় না। নামাজের লগ, বুকমার্ক বা নোট কোথাও আপলোড হয় না।",
                        "SalahMate does not upload anything. Prayer logs, bookmarks and notes never leave this device unless you export them yourself.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }
        }
    }

    if (confirmClearQuran) {
        ConfirmDialog(
            title = t(bn, "কুরআন ক্যাশ মুছবেন?", "Clear Quran cache?"),
            message = t(bn, "ডাউনলোড করা অনুবাদ ও অডিও মুছে যাবে। আরবি টেক্সট অ্যাপের সাথে থাকে, তাই কুরআন পড়া যাবে।", "Downloaded translations and audio will be removed. The Arabic text ships with the app, so the reader still works."),
            onConfirm = { confirmClearQuran = false; viewModel.clearQuranCache() },
            onDismiss = { confirmClearQuran = false },
            confirmLabel = t(bn, "মুছুন", "Clear"),
        )
    }
    if (confirmClearHadith) {
        ConfirmDialog(
            title = t(bn, "হাদিস ডাউনলোড মুছবেন?", "Delete hadith downloads?"),
            message = t(bn, "ডাউনলোড করা সংকলনগুলো মুছে যাবে; বান্ডল করা সংকলন থাকবে।", "Downloaded collections will be deleted; the bundled collections remain."),
            onConfirm = { confirmClearHadith = false; viewModel.clearHadithDownloads() },
            onDismiss = { confirmClearHadith = false },
            confirmLabel = t(bn, "মুছুন", "Delete"),
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit, confirmLabel: String) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
