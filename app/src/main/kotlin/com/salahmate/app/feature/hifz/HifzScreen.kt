package com.salahmate.app.feature.hifz

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.AppContainer
import com.salahmate.app.core.db.HifzItemEntity
import com.salahmate.app.core.db.HifzReviewLogEntity
import com.salahmate.app.core.db.HifzState
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.EmptyState
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.components.StatusPill
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.tracking.HifzScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HifzUiState(val message: String? = null)

/**
 * Hifz tracker. Review intervals come from [HifzScheduler]; the app schedules nothing in the
 * background for reviews — it shows what is due when the user opens the screen.
 */
class HifzViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(HifzUiState())
    val ui: StateFlow<HifzUiState> = _ui

    val items: StateFlow<List<HifzItemEntity>> = container.database.hifzDao().items()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun add(surah: Int, from: Int, to: Int, state: HifzState) {
        if (surah !in 1..114 || from < 1 || to < from) {
            _message.value = "Check the surah and ayah range."
            return
        }
        viewModelScope.launch {
            val existing = container.database.hifzDao().find(surah, from, to)
            if (existing != null) {
                container.database.hifzDao().updateItem(existing.id, state.name, existing.notes)
                _message.value = "That range is already tracked — its state was updated."
                return@launch
            }
            container.database.hifzDao().insert(
                HifzItemEntity(
                    surah = surah,
                    ayahStart = from,
                    ayahEnd = to,
                    state = state.name,
                    lastReviewedAt = null,
                    nextReviewAt = null,
                    reviewCount = 0,
                    notes = null,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            _message.value = "Added surah $surah, ayah $from–$to."
        }
    }

    fun review(item: HifzItemEntity, newState: HifzState) {
        val now = Instant.now()
        val next = HifzScheduler.nextReview(newState.name, item.reviewCount, now)
        viewModelScope.launch {
            container.database.hifzDao().updateReview(item.id, newState.name, now.toEpochMilli(), next.toEpochMilli())
            container.database.hifzDao().logReview(
                HifzReviewLogEntity(
                    date = LocalDate.now().toString(),
                    surah = item.surah,
                    ayahCount = item.ayahEnd - item.ayahStart + 1,
                    resultingState = newState.name,
                    reviewedAt = now.toEpochMilli(),
                ),
            )
            _message.value = "Review saved. Next review on " +
                next.atZone(ZoneId.systemDefault()).toLocalDate().toString() + "."
        }
    }

    fun updateState(item: HifzItemEntity, newState: HifzState) {
        viewModelScope.launch {
            container.database.hifzDao().updateItem(item.id, newState.name, item.notes)
        }
    }

    fun setNotes(item: HifzItemEntity, notes: String) {
        viewModelScope.launch {
            container.database.hifzDao().updateNotes(item.id, notes.ifBlank { null })
            _message.value = "Note saved."
        }
    }

    fun delete(item: HifzItemEntity) {
        viewModelScope.launch {
            container.database.hifzDao().delete(item.id)
            _message.value = "Removed from your hifz list."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun dueNow(items: List<HifzItemEntity>): List<HifzItemEntity> {
        val now = Instant.now()
        return items.filter { item ->
            HifzScheduler.isDue(item.nextReviewAt?.let { Instant.ofEpochMilli(it) }, now)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HifzScreen(viewModel: HifzViewModel, settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val items by viewModel.items.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val due = viewModel.dueNow(items)

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "হিফজ ট্র্যাকার", "Hifz tracker")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = t(bn, "যোগ করুন", "Add range"))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t(bn, "পুনরাবৃত্তি বাকি", "Due for review"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${due.size} " + t(bn, "টি অংশ", "portions"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusPill(text = "${items.count { it.state == HifzState.MEMORIZED.name }}" + " " + t(bn, "মুখস্থ", "memorised"))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(t(bn, "নতুন অংশ যোগ করুন", "Add a portion"))
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        title = t(bn, "এখনো কিছু যোগ করা হয়নি", "Nothing tracked yet"),
                        message = t(bn, "সূরা ও আয়াতের সীমা দিয়ে মুখস্থের অংশ যোগ করুন।", "Add a portion using a surah and ayah range."),
                        icon = Icons.Outlined.School,
                    )
                }
            }

            items(items, key = { it.id }) { item ->
                HifzItemCard(
                    item = item,
                    bangla = bn,
                    isDue = due.any { it.id == item.id },
                    onReview = { state -> viewModel.review(item, state) },
                    onDelete = { viewModel.delete(item) },
                )
            }

            item {
                InfoBanner(
                    text = t(
                        bn,
                        "পুনরাবৃত্তির ব্যবধান: মুখস্থ ১/৩/৭/১৪/৩০/৬০ দিন; শিখছি ও সংশোধন প্রয়োজন ১ দিন। আয়াতের টেক্সট এই স্ক্রিনে দেখানো হয় না — পাঠের জন্য কুরআন রিডার ব্যবহার করুন।",
                        "Review spacing: memorised 1/3/7/14/30/60 days; learning and needs-revision 1 day. The ayah text is not duplicated here — read it in the Quran reader.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }
        }
    }

    if (showAdd) {
        AddHifzDialog(
            bangla = bn,
            onDismiss = { showAdd = false },
            onAdd = { surah, from, to, state ->
                viewModel.add(surah, from, to, state)
                showAdd = false
            },
        )
    }
}

@Composable
private fun HifzItemCard(
    item: HifzItemEntity,
    bangla: Boolean,
    isDue: Boolean,
    onReview: (HifzState) -> Unit,
    onDelete: () -> Unit,
) {
    val state = runCatching { HifzState.valueOf(item.state) }.getOrDefault(HifzState.LEARNING)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    t(bangla, "সূরা ", "Surah ") + "${item.surah}" + ", " +
                        t(bangla, "আয়াত ", "ayah ") + "${item.ayahStart}–${item.ayahEnd}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stateLabel(state, bangla) + " · " + t(bangla, "পুনরাবৃত্তি ", "reviews ") + "${item.reviewCount}" +
                        (item.nextReviewAt?.let {
                            " · " + t(bangla, "পরবর্তী ", "next ") +
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isDue) StatusPill(text = t(bangla, "বাকি", "Due"), color = MaterialTheme.colorScheme.error)
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = t(bangla, "মুছুন", "Delete"), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(t(bangla, "পুনরাবৃত্তি সম্পন্ন করে অবস্থা বাছুন:", "Mark a review with its new state:"), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HifzState.entries.forEach { candidate ->
                FilterChip(
                    selected = false,
                    onClick = { onReview(candidate) },
                    label = { Text(stateLabel(candidate, bangla)) },
                )
            }
        }
    }
}

private fun stateLabel(state: HifzState, bangla: Boolean): String = when (state) {
    HifzState.MEMORIZED -> if (bangla) "মুখস্থ" else "Memorised"
    HifzState.LEARNING -> if (bangla) "শিখছি" else "Learning"
    HifzState.NEEDS_REVISION -> if (bangla) "সংশোধন দরকার" else "Needs revision"
}

@Composable
private fun AddHifzDialog(
    bangla: Boolean,
    onDismiss: () -> Unit,
    onAdd: (Int, Int, Int, HifzState) -> Unit,
) {
    var surah by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(HifzState.LEARNING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(bangla, "মুখস্থের অংশ যোগ করুন", "Add a portion")) },
        text = {
            Column {
                OutlinedTextField(
                    value = surah,
                    onValueChange = { surah = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(t(bangla, "সূরা নম্বর (১–১১৪)", "Surah number (1–114)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = from,
                        onValueChange = { from = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text(t(bangla, "শুরুর আয়াত", "From ayah")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = to,
                        onValueChange = { to = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text(t(bangla, "শেষ আয়াত", "To ayah")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HifzState.entries.forEach { candidate ->
                        FilterChip(
                            selected = state == candidate,
                            onClick = { state = candidate },
                            label = { Text(stateLabel(candidate, bangla)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = surah.toIntOrNull() ?: 0
                    val f = from.toIntOrNull() ?: 0
                    val t2 = to.toIntOrNull() ?: f
                    onAdd(s, f, t2, state)
                },
            ) { Text(t(bangla, "যোগ করুন", "Add")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t(bangla, "বাতিল", "Cancel")) } },
    )
}
