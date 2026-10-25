package com.salahmate.app.feature.mosque

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.EmptyState
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import kotlinx.coroutines.launch

/**
 * Mosque finder.
 *
 * The app ships no mosque database and does not fabricate a list of nearby mosques. Instead it
 * hands the search to an installed maps application through a standard geo/view intent — the maps
 * app owns the place data and its accuracy. This is a provider abstraction: if a mosque data
 * source is ever added, it plugs in behind [MosqueProvider] without changing this screen.
 */
interface MosqueProvider {
    val id: String
    val label: String
    fun intentFor(context: Context, latitude: Double, longitude: Double, query: String?): Intent
}

/** Opens the user's default maps app searching for mosques near the coordinates. */
object MapsSearchProvider : MosqueProvider {
    override val id = "maps.search"
    override val label = "Maps app search"

    override fun intentFor(context: Context, latitude: Double, longitude: Double, query: String?): Intent {
        val q = query ?: "mosque"
        val uri = Uri.parse("geo:$latitude,$longitude?q=" + Uri.encode("$q near $latitude,$longitude"))
        return Intent(Intent.ACTION_VIEW, uri)
    }
}

/** Plain map view without a keyword search, for devices whose maps app ignores `geo:` queries. */
object MapsLocationProvider : MosqueProvider {
    override val id = "maps.location"
    override val label = "Maps app at my location"

    override fun intentFor(context: Context, latitude: Double, longitude: Double, query: String?): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/" + Uri.encode(query ?: "mosque") + "/@$latitude,$longitude,15z"),
        )
}

object MosqueProviders {
    val all: List<MosqueProvider> = listOf(MapsSearchProvider, MapsLocationProvider)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueScreen(settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun launch(provider: MosqueProvider, query: String?) {
        val intent = provider.intentFor(context, settings.latitude, settings.longitude, query)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch {
                snackbar.showSnackbar("No maps application is installed on this device.")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "মসজিদ খুঁজুন", "Mosque finder")) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AppCard {
                    Text(t(bn, "আপনার অবস্থান", "Your location"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        settings.locationLabel + " · %.4f, %.4f".format(settings.latitude, settings.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { launch(MapsSearchProvider, "mosque") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Mosque, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(t(bn, "কাছের মসজিদ ম্যাপে দেখুন", "Show nearby mosques in a maps app"))
                    }
                }
            }

            item {
                Text(t(bn, "অন্য কিছু খুঁজুন", "Search for something else"), style = MaterialTheme.typography.titleMedium)
            }
            items(
                listOf(
                    "mosque" to (t(bn, "মসজিদ", "Mosque") to t(bn, "মসজিদ", "Mosque")),
                    "islamic centre" to (t(bn, "ইসলামিক সেন্টার", "Islamic centre") to t(bn, "ইসলামিক সেন্টার", "Islamic centre")),
                    "janazah" to (t(bn, "জানাজার স্থান", "Funeral / janazah facility") to t(bn, "জানাজা", "Janazah")),
                    "halal restaurant" to (t(bn, "হালাল রেস্টুরেন্ট", "Halal restaurant") to t(bn, "হালাল", "Halal")),
                ),
            ) { (query, labels) ->
                AppCard(modifier = Modifier.clickable { launch(MapsSearchProvider, query) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(labels.first, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                t(bn, "ম্যাপ অ্যাপে খুঁজুন", "Open in a maps app"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Text(t(bn, "সরাসরি ম্যাপে", "Open a map directly"), style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedButton(
                    onClick = { launch(MapsLocationProvider, "mosque") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(t(bn, "ব্রাউজারে ম্যাপ খুলুন", "Open the map in a browser tab")) }
            }

            item {
                InfoBanner(
                    text = t(
                        bn,
                        "অ্যাপে কোনো মসজিদের তালিকা সংরক্ষিত নেই — সার্চ আপনার ইনস্টল করা ম্যাপ অ্যাপে হয়, তাই তথ্যের নির্ভুলতা ও আপডেট সেই অ্যাপের ওপর নির্ভর করে। কোনো ম্যাপ অ্যাপ না থাকলে উপরের বাটনটি ব্রাউজারে খোলে।",
                        "No mosque list is bundled with the app — the search happens in your installed maps app, so listing accuracy depends on that app. If no maps app exists, the button above opens a browser tab instead.",
                    ),
                    icon = Icons.Outlined.Info,
                )
            }
        }
    }
}
