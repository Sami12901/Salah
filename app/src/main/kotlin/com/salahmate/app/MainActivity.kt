package com.salahmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.salahmate.app.core.design.SalahMateTheme
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.feature.onboarding.OnboardingScreen
import com.salahmate.app.feature.onboarding.OnboardingViewModel
import com.salahmate.app.ui.SalahMateApp
import com.salahmate.app.ui.containerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the theme hand-off happens on the very first frame.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as SalahMateApplication).container
        setContent {
            val settings by container.settings.settings.collectAsState(initial = UserSettings())
            CompositionLocalProvider(
                LocalLayoutDirection provides if (settings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                SalahMateTheme(
                    themeMode = settings.themeMode,
                    arabicScale = settings.arabicScale,
                    showTranslation = settings.showTranslation,
                ) {
                    if (settings.onboardingCompleted) {
                        SalahMateApp(container = container, settings = settings)
                    } else {
                        // First run: choose language, location, method and reminders before the
                        // dashboard shows a schedule that came from somewhere the user never picked.
                        val viewModel = containerViewModel(container) { OnboardingViewModel(it) }
                        OnboardingScreen(
                            viewModel = viewModel,
                            onFinished = { /* settings flow flips onboardingCompleted and recomposes */ },
                        )
                    }
                }
            }
        }
    }
}
