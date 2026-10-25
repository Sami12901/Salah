package com.salahmate.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.salahmate.app.AppContainer
import com.salahmate.app.Routes
import com.salahmate.app.core.design.t
import com.salahmate.app.core.design.t3
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.feature.about.AboutScreen
import com.salahmate.app.feature.calendar.CalendarScreen
import com.salahmate.app.feature.dua.DuaScreen
import com.salahmate.app.feature.dua.DuaViewModel
import com.salahmate.app.feature.hadith.HadithLibraryScreen
import com.salahmate.app.feature.hadith.HadithViewModel
import com.salahmate.app.feature.hifz.HifzScreen
import com.salahmate.app.feature.hifz.HifzViewModel
import com.salahmate.app.feature.home.HomeScreen
import com.salahmate.app.feature.home.HomeViewModel
import com.salahmate.app.feature.hubs.LearnScreen
import com.salahmate.app.feature.hubs.MoreScreen
import com.salahmate.app.feature.hubs.TrackScreen
import com.salahmate.app.feature.learning.LearningScreen
import com.salahmate.app.feature.mosque.MosqueScreen
import com.salahmate.app.feature.prayer.PrayerTrackingScreen
import com.salahmate.app.feature.prayer.TimetableScreen
import com.salahmate.app.feature.prayer.TimetableViewModel
import com.salahmate.app.feature.prayer.PrayerTrackingViewModel
import com.salahmate.app.feature.qibla.QiblaScreen
import com.salahmate.app.feature.quran.QuranReaderScreen
import com.salahmate.app.feature.quran.QuranViewModel
import com.salahmate.app.feature.quran.SurahListScreen
import com.salahmate.app.feature.ramadan.RamadanScreen
import com.salahmate.app.feature.ramadan.RamadanViewModel
import com.salahmate.app.feature.settings.DataSettingsScreen
import com.salahmate.app.feature.settings.NotificationSettingsScreen
import com.salahmate.app.feature.settings.QuranSettingsScreen
import com.salahmate.app.feature.settings.SettingsScreen
import com.salahmate.app.feature.settings.SettingsViewModel
import com.salahmate.app.feature.tasbih.TasbihScreen
import com.salahmate.app.feature.tasbih.TasbihViewModel
import com.salahmate.app.feature.zakat.ZakatScreen
import com.salahmate.app.feature.zakat.ZakatViewModel

private data class TabItem(val route: String, val icon: ImageVector, val labelEn: String, val labelBn: String, val labelAr: String)

private val tabs = listOf(
    TabItem(Routes.HOME, Icons.Outlined.Home, "Home", "হোম", "الرئيسية"),
    TabItem(Routes.QURAN, Icons.Outlined.MenuBook, "Quran", "কুরআন", "القرآن"),
    TabItem(Routes.TRACK, Icons.Outlined.SelfImprovement, "Track", "ট্র্যাক", "التتبع"),
    TabItem(Routes.LEARN, Icons.Outlined.AutoStories, "Learn", "শিখুন", "التعلّم"),
    TabItem(Routes.MORE, Icons.Outlined.MoreHoriz, "More", "আরও", "المزيد"),
)

@Composable
fun SalahMateApp(container: AppContainer, settings: UserSettings) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }
    val bn = settings.isBanglaUi

    // On a secondary tab, Back returns to Home before leaving the app — otherwise a user who opened
    // "Track" once would exit Salah from a screen that is not the start destination.
    androidx.activity.compose.BackHandler(
        enabled = currentRoute != null && currentRoute != Routes.HOME && showBottomBar,
    ) {
        navController.navigateToTab(Routes.HOME)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(t3(bn, settings.isArabicUi, tab.labelBn, tab.labelEn, tab.labelAr)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            // ------------------------------------------------------------ tabs --
            composable(Routes.HOME) {
                val vm = containerViewModel(container) { HomeViewModel(it) }
                val state by vm.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onNavigate = { route -> navController.push(route) },
                )
            }

            composable(Routes.QURAN) {
                val vm = containerViewModel(container) { QuranViewModel(it) }
                SurahListScreen(
                    viewModel = vm,
                    settings = settings,
                    onOpenSurah = { surah, ayah -> navController.push(Routes.reader(surah, ayah)) },
                )
            }

            composable(Routes.TRACK) {
                TrackScreen(settings = settings, onNavigate = { navController.navigate(it) })
            }

            composable(Routes.LEARN) {
                LearnScreen(settings = settings, onNavigate = { navController.navigate(it) })
            }

            composable(Routes.MORE) {
                MoreScreen(settings = settings, onNavigate = { navController.navigate(it) })
            }

            // ----------------------------------------------------------- Quran --
            composable(
                route = Routes.QURAN_READER,
                arguments = listOf(
                    navArgument("surah") { type = NavType.IntType },
                    navArgument("ayah") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val vm = containerViewModel(container) { QuranViewModel(it) }
                val surah = entry.arguments?.getInt("surah") ?: 1
                val ayah = entry.arguments?.getInt("ayah") ?: 0
                QuranReaderScreen(
                    viewModel = vm,
                    settings = settings,
                    surahNumber = surah,
                    startAyah = ayah.takeIf { it > 0 },
                    onBack = { navController.popBackStack() },
                )
            }

            // ---------------------------------------------------------- Track ----
            composable(Routes.PRAYER_TRACKING) {
                val vm = containerViewModel(container) { PrayerTrackingViewModel(it) }
                PrayerTrackingScreen(vm, settings) { navController.popBackStack() }
            }
            composable(Routes.HIFZ) {
                val vm = containerViewModel(container) { HifzViewModel(it) }
                HifzScreen(vm, settings) { navController.popBackStack() }
            }
            composable(Routes.TASBIH) {
                val vm = containerViewModel(container) { TasbihViewModel(it) }
                TasbihScreen(vm, settings) { navController.popBackStack() }
            }

            // ---------------------------------------------------------- Learn ----
            composable(Routes.HADITH) {
                val vm = containerViewModel(container) { HadithViewModel(it) }
                HadithLibraryScreen(vm, settings) { navController.popBackStack() }
            }
            composable(Routes.DUA) {
                val vm = containerViewModel(container) { DuaViewModel(it) }
                DuaScreen(vm, settings) { navController.popBackStack() }
            }
            composable(Routes.LEARNING) {
                LearningScreen(settings) { navController.popBackStack() }
            }

            // ----------------------------------------------------------- More ----
            composable(Routes.QIBLA) {
                QiblaScreen(settings) { navController.popBackStack() }
            }
            composable(Routes.ZAKAT) {
                val vm = containerViewModel(container) { ZakatViewModel(it) }
                ZakatScreen(vm, settings) { navController.popBackStack() }
            }
            composable(Routes.RAMADAN) {
                val vm = containerViewModel(container) { RamadanViewModel(it) }
                RamadanScreen(vm, settings) { navController.popBackStack() }
            }
            composable(Routes.TIMETABLE) {
                val vm = containerViewModel(container) { TimetableViewModel(it) }
                TimetableScreen(
                    viewModel = vm,
                    settings = settings,
                    onOpenSettings = { navController.push(Routes.SETTINGS) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(settings) { navController.popBackStack() }
            }
            composable(Routes.MOSQUE) {
                MosqueScreen(settings) { navController.popBackStack() }
            }

            // -------------------------------------------------------- Settings ---
            composable(Routes.ABOUT) {
                AboutScreen(settings) { navController.popBackStack() }
            }
            composable(Routes.PRAYER_SETTINGS) {
                // Location, calculation method and adjustments all live on the main settings screen.
                val vm = containerViewModel(container) { SettingsViewModel(it, it.appContext) }
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNavigate = { navController.push(it) },
                )
            }
            composable(Routes.SETTINGS) {
                val vm = containerViewModel(container) { SettingsViewModel(it, it.appContext) }
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNavigate = { navController.push(it) },
                )
            }
            composable(Routes.NOTIFICATION_SETTINGS) {
                val vm = containerViewModel(container) { SettingsViewModel(it, it.appContext) }
                NotificationSettingsScreen(vm) { navController.popBackStack() }
            }
            composable(Routes.QURAN_SETTINGS) {
                val vm = containerViewModel(container) { SettingsViewModel(it, it.appContext) }
                QuranSettingsScreen(vm) { navController.popBackStack() }
            }
            composable(Routes.DATA_SETTINGS) {
                val vm = containerViewModel(container) { SettingsViewModel(it, it.appContext) }
                DataSettingsScreen(vm) { navController.popBackStack() }
            }
        }
    }
}

/**
 * Pushes a detail screen without stacking duplicates: tapping the same quick action twice returns to
 * the copy already on the back stack instead of creating a second one that "backs" into itself.
 */
private fun NavHostController.push(route: String) {
    navigate(route) { launchSingleTop = true }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
