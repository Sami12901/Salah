package com.salahmate.app.feature.tasbih

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.AppContainer
import com.salahmate.app.core.db.TasbihSessionEntity
import com.salahmate.app.core.design.arabicStyle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.content.DhikrContent
import com.salahmate.app.data.content.DhikrPhrase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tasbih counter. The active session is persisted in Room after every tap, so a process death or
 * a phone restart never loses the count.
 */
class TasbihViewModel(private val container: AppContainer) : ViewModel() {

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    private val _selected = MutableStateFlow(DhikrContent.phrases.first())
    val selected: StateFlow<DhikrPhrase> = _selected

    private val _target = MutableStateFlow(DhikrContent.phrases.first().defaultTarget)
    val target: StateFlow<Int> = _target

    private var sessionId: Long? = null

    val sessions: StateFlow<List<TasbihSessionEntity>> = container.database.tasbihDao().sessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            // Resume an unfinished session rather than silently resetting it.
            val active = container.database.tasbihDao().activeSession()
            if (active != null) {
                sessionId = active.id
                _count.value = active.count
                _target.value = active.target
                DhikrContent.byId(active.dhikrId)?.let { _selected.value = it }
            }
        }
    }

    fun selectPhrase(phrase: DhikrPhrase) {
        _selected.value = phrase
        _target.value = phrase.defaultTarget
        _count.value = 0
        sessionId = null
    }

    fun setTarget(value: Int) {
        _target.value = value.coerceIn(1, 10_000)
    }

    fun increment() {
        _count.value += 1
        val current = _count.value
        viewModelScope.launch {
            val id = sessionId
            if (id == null) {
                sessionId = container.database.tasbihDao().insert(
                    TasbihSessionEntity(
                        dhikrId = _selected.value.id,
                        target = _target.value,
                        count = current,
                        startedAt = System.currentTimeMillis()
                    )
                )
            } else {
                container.database.tasbihDao().updateCount(id, current)
            }
        }
    }

    fun reset() {
        _count.value = 0
        val id = sessionId
        viewModelScope.launch {
            if (id != null) {
                container.database.tasbihDao().updateCount(id, 0)
            }
        }
    }

    fun finish(note: String? = null) {
        val id = sessionId ?: return
        val countNow = _count.value
        viewModelScope.launch {
            container.database.tasbihDao().complete(id, System.currentTimeMillis(), note)
            sessionId = null
            _count.value = 0
        }
    }

    fun delete(session: TasbihSessionEntity) = viewModelScope.launch {
        container.database.tasbihDao().delete(session)
        if (session.id == sessionId) {
            sessionId = null
            _count.value = 0
        }
    }

    fun total(sessions: List<TasbihSessionEntity>): Int = sessions.sumOf { it.count }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(viewModel: TasbihViewModel, settings: UserSettings, onBack: () -> Unit) {
    val count by viewModel.count.collectAsStateWithLifecycle()
    val phrase by viewModel.selected.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val bn = settings.isBanglaUi
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    val vibrator = remember {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
    }
    fun haptic() {
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(18)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "তাসবিহ", "Tasbih")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = t(bn, "রিসেট", "Reset"))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Text(
                        phrase.arabic,
                        style = arabicStyle(sizeSp = 30f),
                        textAlign = TextAlign.Center,
    
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(phrase.transliteration, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(
                        if (bn) phrase.bengali else phrase.english,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        phrase.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(220.dp)
                            .clickable {
                                haptic()
                                viewModel.increment()
                            }
                            .semantics {
                                contentDescription = "Tap to count. Current count $count of $target"
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "$count",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                t(bn, "লক্ষ্য ", "Target ") + "$target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Text(
                    t(bn, "গুনতে সার্কেলে ট্যাপ করুন", "Tap the circle to count"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showResetDialog = true }, modifier = Modifier.weight(1f)) {
                        Text(t(bn, "রিসেট", "Reset"))
                    }
                    Button(onClick = { viewModel.finish() }, modifier = Modifier.weight(1f)) {
                        Text(t(bn, "সেশন শেষ করুন", "Finish session"))
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "লক্ষ্য", "Target"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(33, 34, 100, 500, 1000).forEach { value ->
                            FilterChip(
                                selected = target == value,
                                onClick = { viewModel.setTarget(value) },
                                label = { Text("$value") }
                            )
                        }
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "যিকর বাছুন", "Choose a dhikr"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    DhikrContent.phrases.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectPhrase(item) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = item.id == phrase.id,
                                onClick = { viewModel.selectPhrase(item) }
                            )
                            Column {
                                Text(item.arabic, style = arabicStyle(sizeSp = 18f), )
                                Text(
                                    if (bn) item.bengali else item.english,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t(bn, "সেশন ইতিহাস", "Session history"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            "${sessions.size} " + t(bn, "সেশন", "sessions"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    if (sessions.isEmpty()) {
                        Text(
                            t(bn, "এখনো কোনো সেশন নেই।", "No sessions yet."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val formatter = remember { SimpleDateFormat("d MMM yyyy, HH:mm", if (bn) Locale("bn", "BD") else Locale.ENGLISH) }
                        sessions.take(20).forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        DhikrContent.byId(session.dhikrId)?.transliteration ?: session.dhikrId,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        formatter.format(Date(session.startedAt)) + " · " + "${session.count}/${session.target}" +
                                            if (session.completedAt != null) " · " + t(bn, "সম্পন্ন", "finished") else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.delete(session) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = t(bn, "মুছুন", "Delete"), modifier = Modifier.size(18.dp))
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
                        "গণনা স্বয়ংক্রিয়ভাবে সংরক্ষিত হয় — অ্যাপ বন্ধ করলেও বা ফোন রিস্টার্ট করলেও হারাবে না।",
                        "The count is saved automatically — closing the app or restarting the phone does not lose it."
                    ),
                    icon = Icons.Outlined.Info
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(t(bn, "কাউন্টার রিসেট করবেন?", "Reset the counter?")) },
            text = { Text(t(bn, "বর্তমান গণনা শূন্য হবে। সংরক্ষিত সেশন ইতিহাস মুছে যাবে না।", "The current count returns to zero. Saved session history is kept.")) },
            confirmButton = {
                TextButton(onClick = { viewModel.reset(); showResetDialog = false }) {
                    Text(t(bn, "রিসেট", "Reset"))
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(t(bn, "বাতিল", "Cancel")) } }
        )
    }
}
