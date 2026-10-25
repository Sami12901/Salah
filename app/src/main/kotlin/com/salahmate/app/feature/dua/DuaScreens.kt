package com.salahmate.app.feature.dua

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salahmate.app.AppContainer
import com.salahmate.app.core.db.DuaCompletionEntity
import com.salahmate.app.core.db.DuaFavoriteEntity
import com.salahmate.app.core.design.arabicStyle
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.components.StatusPill
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.data.content.DuaContent
import com.salahmate.app.data.content.DuaEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Dua & Adhkar. Content is a bundled, attributed dataset; favourites and daily ticks are local. */
class DuaViewModel(private val container: AppContainer) : ViewModel() {

    val favorites: StateFlow<Set<String>> = container.database.duaDao().favorites()
        .map { rows -> rows.map { it.duaId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _doneToday = MutableStateFlow<Set<String>>(emptySet())
    val doneToday: StateFlow<Set<String>> = _doneToday

    init {
        viewModelScope.launch {
            container.database.duaDao().completionsOn(LocalDate.now().toString()).collect { rows ->
                _doneToday.value = rows.map { it.duaId }.toSet()
            }
        }
    }

    fun toggleFavorite(duaId: String) {
        viewModelScope.launch {
            if (favorites.value.contains(duaId)) {
                container.database.duaDao().unfavorite(duaId)
            } else {
                container.database.duaDao().favorite(DuaFavoriteEntity(duaId, System.currentTimeMillis()))
            }
        }
    }

    fun toggleDone(duaId: String) {
        val date = LocalDate.now().toString()
        viewModelScope.launch {
            if (_doneToday.value.contains(duaId)) {
                container.database.duaDao().uncomplete(duaId, date)
            } else {
                container.database.duaDao().complete(DuaCompletionEntity(duaId, date, System.currentTimeMillis()))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaScreen(viewModel: DuaViewModel, settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val done by viewModel.doneToday.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf(DuaContent.categories.first().id) }
    var onlyFavorites by remember { mutableStateOf(false) }

    val entries = if (onlyFavorites) {
        DuaContent.entries.filter { favorites.contains(it.id) }
    } else {
        DuaContent.byCategory(category)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "দুআ ও আযকার", "Dua & Adhkar")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t(bn, "ফিরে যান", "Back"))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                InfoBanner(
                    text = t(
                        bn,
                        "সব দুআ অ্যাপের ভেতরে সংরক্ষিত, ইন্টারনেট ছাড়াই পড়া যায়। প্রতিটির নিচে উৎস দেওয়া আছে; কোনো দুআ নিজে থেকে যোগ করা হয়নি।",
                        "All supplications are bundled with the app and work offline. Each shows its source; nothing is added at runtime."
                    ),
                    icon = Icons.Outlined.Info
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = { onlyFavorites = !onlyFavorites },
                        label = { Text(t(bn, "প্রিয়", "Favourites") + " (${favorites.size})") }
                    )
                    FilterChip(
                        selected = !onlyFavorites,
                        onClick = { onlyFavorites = false },
                        label = { Text(t(bn, "বিভাগ", "Categories")) }
                    )
                }
            }
            if (!onlyFavorites) {
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DuaContent.categories) { cat ->
                            FilterChip(
                                selected = category == cat.id,
                                onClick = { category = cat.id },
                                label = { Text(if (bn) cat.titleBn else cat.titleEn) }
                            )
                        }
                    }
                }
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        t(bn, "এই বিভাগে কোনো দুআ নেই।", "No supplications in this section yet."),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(entries, key = { it.id }) { entry ->
                DuaCard(
                    entry = entry,
                    bangla = bn,
                    favorite = favorites.contains(entry.id),
                    done = done.contains(entry.id),
                    onFavorite = { viewModel.toggleFavorite(entry.id) },
                    onDone = { viewModel.toggleDone(entry.id) }
                )
            }
            item {
                Text(
                    DuaContent.attribution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DuaCard(
    entry: DuaEntry,
    bangla: Boolean,
    favorite: Boolean,
    done: Boolean,
    onFavorite: () -> Unit,
    onDone: () -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (bangla) DuaContent.categoryById(entry.categoryId)?.titleBn ?: "" else DuaContent.categoryById(entry.categoryId)?.titleEn ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (entry.repeat > 1) {
                StatusPill(text = "${entry.repeat}×")
                Spacer(Modifier.size(6.dp))
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (favorite) "Remove from favourites" else "Add to favourites",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.arabic,
            style = arabicStyle(sizeSp = 24f),
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(entry.transliteration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(if (bangla) entry.bengali else entry.english, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            t(bangla, "উৎস: ", "Source: ") + entry.source + (entry.quranReference?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (entry.repeatNote.isNotBlank()) {
            Text(entry.repeatNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onDone)) {
            Checkbox(checked = done, onCheckedChange = { onDone() })
            Text(t(bangla, "আজ পড়েছি", "Read today"), style = MaterialTheme.typography.bodySmall)
        }
    }
}
