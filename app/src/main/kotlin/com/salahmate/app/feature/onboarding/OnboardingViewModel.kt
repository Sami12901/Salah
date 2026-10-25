package com.salahmate.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salahmate.app.AppContainer
import com.salahmate.app.core.location.City
import com.salahmate.app.core.location.Cities
import com.salahmate.app.core.location.LocationResult
import com.salahmate.app.core.prefs.AppLanguage
import com.salahmate.app.core.prefs.AsrMadhab
import com.salahmate.app.core.prefs.CalculationMethodId
import com.salahmate.app.core.prefs.LocationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val language: AppLanguage = AppLanguage.BENGALI,
    val selectedCity: City = Cities.bangladesh.first(),
    val cityQuery: String = "",
    val locationMode: LocationMode = LocationMode.MANUAL,
    val latitude: Double = 23.8103,
    val longitude: Double = 90.4125,
    val locationLabel: String = "Dhaka, Bangladesh",
    val timeZoneId: String = "Asia/Dhaka",
    val method: CalculationMethodId = CalculationMethodId.KARACHI,
    val madhab: AsrMadhab = AsrMadhab.HANAFI,
    val notificationsEnabled: Boolean = true,
    val locating: Boolean = false,
    val locationMessage: String? = null,
    val finishing: Boolean = false,
) {
    val totalSteps: Int get() = 3
    val canGoBack: Boolean get() = step > 0
    val isLastStep: Boolean get() = step == totalSteps - 1
}

/**
 * First-run setup: language, location, calculation method and reminder opt-in.
 *
 * Every value written here is the same setting the rest of the app reads, so nothing chosen in
 * onboarding has to be re-entered later, and skipping is possible without breaking anything (the
 * defaults are Dhaka + Karachi + Hanafi, clearly labelled).
 */
class OnboardingViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui: StateFlow<OnboardingUiState> = _ui

    fun setLanguage(language: AppLanguage) {
        _ui.value = _ui.value.copy(language = language)
    }

    fun setCityQuery(query: String) {
        _ui.value = _ui.value.copy(cityQuery = query)
    }

    fun cities(): List<City> = Cities.search(_ui.value.cityQuery, _ui.value.language == AppLanguage.BENGALI)

    fun selectCity(city: City) {
        _ui.value = _ui.value.copy(
            selectedCity = city,
            locationMode = LocationMode.MANUAL,
            latitude = city.latitude,
            longitude = city.longitude,
            locationLabel = city.label,
            timeZoneId = city.timeZoneId,
            locationMessage = null,
        )
    }

    fun setManualCoordinates(latitude: Double, longitude: Double) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            _ui.value = _ui.value.copy(locationMessage = "Latitude must be -90…90 and longitude -180…180.")
            return
        }
        val nearest = Cities.nearest(latitude, longitude)
        _ui.value = _ui.value.copy(
            locationMode = LocationMode.MANUAL,
            latitude = latitude,
            longitude = longitude,
            locationLabel = "%.4f, %.4f".format(latitude, longitude),
            timeZoneId = nearest.timeZoneId,
            locationMessage = "Using manual coordinates. Timezone set to ${nearest.timeZoneId} — change it in Settings if that is wrong.",
        )
    }

    fun useDeviceLocation() {
        if (_ui.value.locating) return
        _ui.value = _ui.value.copy(locating = true, locationMessage = null)
        viewModelScope.launch {
            when (val result = container.locationProvider.currentLocation()) {
                is LocationResult.Success -> {
                    val fix = result.location
                    val city = Cities.nearest(fix.latitude, fix.longitude)
                    _ui.value = _ui.value.copy(
                        locating = false,
                        locationMode = LocationMode.GPS,
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        locationLabel = city.label,
                        timeZoneId = city.timeZoneId,
                        locationMessage = "Location detected near ${city.label}.",
                    )
                }
                LocationResult.PermissionDenied -> _ui.value = _ui.value.copy(
                    locating = false,
                    locationMessage = "Location permission was not granted. Pick a city instead.",
                )
                LocationResult.ProviderDisabled -> _ui.value = _ui.value.copy(
                    locating = false,
                    locationMessage = "Location services are off. Turn them on or pick a city.",
                )
                LocationResult.Timeout -> _ui.value = _ui.value.copy(
                    locating = false,
                    locationMessage = "Location timed out. Pick a city instead.",
                )
                LocationResult.Unavailable -> _ui.value = _ui.value.copy(
                    locating = false,
                    locationMessage = "This device could not provide a location. Pick a city instead.",
                )
            }
        }
    }

    fun setMethod(method: CalculationMethodId) {
        _ui.value = _ui.value.copy(method = method)
    }

    fun setMadhab(madhab: AsrMadhab) {
        _ui.value = _ui.value.copy(madhab = madhab)
    }

    fun setNotifications(enabled: Boolean) {
        _ui.value = _ui.value.copy(notificationsEnabled = enabled)
    }

    fun next() {
        val ui = _ui.value
        if (ui.step < ui.totalSteps - 1) _ui.value = ui.copy(step = ui.step + 1)
    }

    fun back() {
        val ui = _ui.value
        if (ui.step > 0) _ui.value = ui.copy(step = ui.step - 1)
    }

    /** Writes the chosen settings and marks onboarding done. */
    fun finish(onDone: () -> Unit) {
        val ui = _ui.value
        if (ui.finishing) return
        _ui.value = ui.copy(finishing = true)
        viewModelScope.launch {
            container.settings.setLanguage(ui.language)
            container.settings.setLocation(
                mode = ui.locationMode,
                lat = ui.latitude,
                lon = ui.longitude,
                label = ui.locationLabel,
                timeZoneId = ui.timeZoneId,
                city = ui.selectedCity.nameEn,
                country = ui.selectedCity.countryEn,
            )
            container.settings.setMethod(ui.method)
            container.settings.setMadhab(ui.madhab)
            // Reminders stay off until the OS permission actually exists; the settings screen reports it.
            container.settings.setNotificationsEnabled(ui.notificationsEnabled)
            container.settings.setOnboardingCompleted(true)
            onDone()
        }
    }
}
