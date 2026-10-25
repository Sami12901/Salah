package com.salahmate.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.TimeZone
import kotlin.coroutines.resume

/** A single resolved device position. Coordinates are used once and never stored historically. */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
)

sealed interface LocationResult {
    data class Success(val location: DeviceLocation) : LocationResult
    data object PermissionDenied : LocationResult
    data object ProviderDisabled : LocationResult
    data object Timeout : LocationResult
    data object Unavailable : LocationResult
}

/**
 * Location is strictly opt-in. Manual city/coordinate selection is always available and is the
 * default, so the app works with the location permission permanently denied.
 *
 * Only a single fix is requested (no persistent location updates, no background tracking), and the
 * fix is not written to any database.
 */
class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (t: SecurityException) {
            false
        }
    }

    suspend fun currentLocation(timeoutMs: Long = 12_000): LocationResult = withContext(Dispatchers.Main) {
        if (!hasPermission()) return@withContext LocationResult.PermissionDenied
        if (!isLocationEnabled()) return@withContext LocationResult.ProviderDisabled
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext LocationResult.Unavailable

        val lastKnown = lastKnown(manager)
        val fresh = requestSingleFix(manager, timeoutMs)
        val chosen = fresh ?: lastKnown ?: return@withContext LocationResult.Timeout
        val zone = TimeZone.getDefault().id
        LocationResult.Success(DeviceLocation(chosen.latitude, chosen.longitude, zone))
    }

    @Suppress("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? = try {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        providers.mapNotNull { p -> runCatching { manager.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    } catch (t: SecurityException) {
        null
    }

    @Suppress("MissingPermission")
    private suspend fun requestSingleFix(manager: LocationManager, timeoutMs: Long): Location? {
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                try {
                    manager.requestSingleUpdate(provider, { location ->
                        if (cont.isActive) cont.resume(location)
                    }, null)
                    cont.invokeOnCancellation { runCatching { manager.removeUpdates { } } }
                } catch (t: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                } catch (t: IllegalArgumentException) {
                    if (cont.isActive) cont.resume(null)
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }
}
