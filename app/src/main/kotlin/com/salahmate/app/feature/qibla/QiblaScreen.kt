package com.salahmate.app.feature.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salahmate.app.core.design.components.AppCard
import com.salahmate.app.core.design.components.InfoBanner
import com.salahmate.app.core.design.t
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.core.sensors.CompassAccuracy
import com.salahmate.app.core.sensors.CompassMath
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Qibla compass.
 *
 * Bearing is computed with the Adhan library (great-circle bearing to the Kaaba). Device heading
 * uses the rotation-vector sensor (best case) or the accelerometer+magnetometer pair (fallback).
 * The screen is explicit when a sensor is missing or the readings look unreliable, and always
 * offers the plain numeric bearing so the direction is still usable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(settings: UserSettings, onBack: () -> Unit) {
    val bn = settings.isBanglaUi
    val context = LocalContext.current
    val qiblaBearing = remember(settings.latitude, settings.longitude) {
        com.batoulapps.adhan.Qibla(
            com.batoulapps.adhan.Coordinates(settings.latitude, settings.longitude)
        ).direction
    }

    var heading by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotation = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accel = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)
            private var gravity: FloatArray? = null
            private var geomagnetic: FloatArray? = null

            override fun onSensorChanged(event: SensorEvent) {
                accuracy = event.accuracy
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val raw = Math.toDegrees(orientation[0].toDouble())
                    // Smooth on the shortest arc so the dial does not jump at the 0/360 boundary.
                    heading = CompassMath.smooth(heading?.toDouble(), raw).toFloat()
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values.clone()
                    if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values.clone()
                    val g = gravity ?: return
                    val m = geomagnetic ?: return
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, g, m)) {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val raw = Math.toDegrees(orientation[0].toDouble())
                        heading = CompassMath.smooth(heading?.toDouble(), raw).toFloat()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, newAccuracy: Int) {
                accuracy = newAccuracy
            }
        }

        if (manager == null || (rotation == null && (accel == null || magnet == null))) {
            sensorAvailable = false
        } else {
            if (rotation != null) {
                manager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
            } else if (accel != null && magnet != null) {
                manager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
                manager.registerListener(listener, magnet, SensorManager.SENSOR_DELAY_UI)
            }
        }
        onDispose {
            manager?.unregisterListener(listener)
        }
    }

    val aligned = heading?.let { CompassMath.isAligned(qiblaBearing, it.toDouble()) } ?: false
    val guidance = CompassMath.guidanceText(qiblaBearing, heading?.toDouble(), bn)
    val cardinal = heading?.let { CompassMath.cardinalLabel(it.toDouble(), bn) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t(bn, "কিবলা", "Qibla")) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Text(
                        t(bn, "কিবলার দিক (সত্য উত্তর থেকে)", "Qibla direction (from true north)"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${qiblaBearing.roundToInt()}°",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.semantics { contentDescription = "Qibla bearing ${qiblaBearing.roundToInt()} degrees" }
                    )
                    Text(
                        t(bn, "অবস্থান: ", "Location: ") + settings.locationLabel,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!sensorAvailable) {
                item {
                    InfoBanner(
                        text = t(
                            bn,
                            "এই ডিভাইসে কম্পাস সেন্সর পাওয়া যায়নি। উপরের সংখ্যাগত দিক ব্যবহার করে কিবলা নির্ণয় করুন।",
                            "No compass sensor was found on this device. Use the numeric bearing above to find the Qibla."
                        ),
                        icon = Icons.Outlined.Info
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CompassDial(
                            heading = heading,
                            qiblaBearing = qiblaBearing,
                            aligned = aligned
                        )
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (aligned) t(bn, "আপনি কিবলার দিকে আছেন", "You are facing the Qibla")
                            else t(bn, "ফোন ঘুরিয়ে কিবলার দিকে করুন", "Rotate the device towards the Qibla"),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            t(bn, "ডিভাইসের দিক: ", "Device heading: ") +
                                (heading?.roundToInt()?.toString() ?: "—") + "° " + (cardinal ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (CompassMath.accuracyFrom(accuracy) < CompassAccuracy.MEDIUM) {
                    item {
                        InfoBanner(
                            text = t(
                                bn,
                                "কম্পাসের নির্ভুলতা কম। ধাতব বস্তু, চুম্বক বা ইলেকট্রনিক ডিভাইস থেকে দূরে সরে ফোনটিকে ৮ আকৃতিতে ঘুরিয়ে ক্যালিব্রেট করুন।",
                                "Compass accuracy is low. Move away from metal or magnets and calibrate by moving the phone in a figure-eight."
                            ),
                            icon = Icons.Outlined.Info
                        )
                    }
                }
            }

            item {
                AppCard {
                    Text(t(bn, "যেভাবে ব্যবহার করবেন", "How to use"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t(
                            bn,
                            "ফোনটি সমতলভাবে ধরে রাখুন এবং নিজের চারপাশে ঘুরুন। সোনালি তীরটি কাবার দিক দেখায়। কম্পাসের পাঠ চৌম্বকীয় প্রভাব ও ফোনের সেন্সর মানের ওপর নির্ভরশীল, তাই বড় জায়গায় বা ধাতব কাঠামোর কাছে নির্ভুলতা কমতে পারে।",
                            "Hold the phone flat and turn on the spot. The gold arrow points to the Kaaba. Compass readings depend on magnetic interference and sensor quality, so accuracy drops outdoors in some locations or near metal structures."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoBanner(
                        text = t(
                            bn,
                            "এটি একটি সহায়ক নির্দেশনা; সন্দেহ হলে স্থানীয় মসজিদের কিবলা অনুসরণ করুন।",
                            "This is an aid, not a ruling. When in doubt, follow the Qibla direction used by your local mosque."
                        ),
                        icon = Icons.Outlined.Explore
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val url = "https://www.google.com/maps/dir/?api=1&destination=${settings.latitude},${settings.longitude}"
                        runCatching {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(t(bn, "ম্যাপে এই অবস্থান খুলুন", "Open this location in a map")) }
            }
        }
    }
}

@Composable
private fun CompassDial(heading: Float?, qiblaBearing: Double, aligned: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .size(300.dp)
            .semantics {
                contentDescription = if (heading == null) {
                    "Compass waiting for sensor data"
                } else {
                    "Compass. Device heading ${heading.roundToInt()} degrees, Qibla at ${qiblaBearing.roundToInt()} degrees"
                }
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 12f
        drawCircle(color = outline, radius = radius, center = center, style = Stroke(width = 4f))

        // Cardinal ticks
        for (i in 0 until 72) {
            val angle = Math.toRadians(i * 5.0)
            val long = i % 18 == 0
            val inner = radius - if (long) 26f else 14f
            val start = Offset(
                (center.x + inner * Math.sin(angle)).toFloat(),
                (center.y - inner * Math.cos(angle)).toFloat()
            )
            val end = Offset(
                (center.x + radius * Math.sin(angle)).toFloat(),
                (center.y - radius * Math.cos(angle)).toFloat()
            )
            drawLine(color = outline, start = start, end = end, strokeWidth = if (long) 3f else 1.5f)
        }

        // Qibla arrow, rotated against the device heading so it always points to the Kaaba.
        val rotation = (qiblaBearing - (heading ?: 0f)) * Math.PI / 180.0
        val tip = Offset(
            (center.x + radius * 0.78 * Math.sin(rotation)).toFloat(),
            (center.y - radius * 0.78 * Math.cos(rotation)).toFloat()
        )
        val tail = Offset(
            (center.x - radius * 0.18 * Math.sin(rotation)).toFloat(),
            (center.y + radius * 0.18 * Math.cos(rotation)).toFloat()
        )
        drawLine(color = if (aligned) gold else primary, start = tail, end = tip, strokeWidth = 10f)
        drawCircle(color = if (aligned) gold else primary, radius = 16f, center = tip)
        drawCircle(color = Color.White, radius = 6f, center = tip)

        // Device heading marker at the top.
        drawCircle(color = primary, radius = 6f, center = Offset(center.x, center.y - radius))
        drawCircle(color = outline, radius = 8f, center = center, style = Stroke(width = 3f))
    }
}
