package com.salahmate.app.core.sensors

import kotlin.math.abs

/**
 * Compass maths, kept pure so it can be verified without a sensor.
 *
 * Everything is in degrees clockwise from true north. Heading comes from the device rotation-vector
 * sensor; the Qibla bearing comes from the Adhan library. The two are combined here rather than in
 * the Composable so the alignment rule and the smoothing filter are testable.
 */
enum class CompassAccuracy { UNRELIABLE, LOW, MEDIUM, HIGH }

object CompassMath {

    /** Wraps any angle into 0..<360. */
    fun normalize(degrees: Double): Double {
        val mod = degrees % 360.0
        return if (mod < 0) mod + 360.0 else mod
    }

    /** Signed shortest rotation from [from] to [to], in -180..180. */
    fun shortestDelta(from: Double, to: Double): Double {
        val diff = normalize(to) - normalize(from)
        return when {
            diff > 180.0 -> diff - 360.0
            diff < -180.0 -> diff + 360.0
            else -> diff
        }
    }

    /** Absolute angle between two bearings, 0..180. */
    fun angularDistance(a: Double, b: Double): Double = abs(shortestDelta(a, b))

    /** Rotation the arrow on screen needs so it points at the Qibla for the current heading. */
    fun arrowRotation(qiblaBearing: Double, heading: Double?): Double =
        normalize(qiblaBearing - (heading ?: 0.0))

    /** True when the device is within [toleranceDegrees] of the Qibla. */
    fun isAligned(qiblaBearing: Double, heading: Double, toleranceDegrees: Double = 6.0): Boolean =
        angularDistance(qiblaBearing, heading) <= toleranceDegrees

    /**
     * Circular low-pass filter. Averaging raw headings naively breaks at the 0/360 boundary (359°
     * and 1° are 2° apart, not 358°), so the update is applied to the shortest delta instead.
     * [factor] 0..1: higher follows the sensor faster and jitters more.
     */
    fun smooth(previous: Double?, current: Double, factor: Double = 0.18): Double {
        if (previous == null) return normalize(current)
        val clamped = factor.coerceIn(0.0, 1.0)
        return normalize(previous + shortestDelta(previous, current) * clamped)
    }

    /** SensorManager.SENSOR_STATUS_* → a value the UI can explain in words. */
    fun accuracyFrom(sensorStatus: Int): CompassAccuracy = when {
        sensorStatus >= 3 -> CompassAccuracy.HIGH
        sensorStatus == 2 -> CompassAccuracy.MEDIUM
        sensorStatus == 1 -> CompassAccuracy.LOW
        else -> CompassAccuracy.UNRELIABLE
    }

    private val CARDINALS_EN = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    private val CARDINALS_BN = listOf(
        "উত্তর", "উত্তর-পূর্ব", "পূর্ব", "দক্ষিণ-পূর্ব", "দক্ষিণ", "দক্ষিণ-পশ্চিম", "পশ্চিম", "উত্তর-পশ্চিম",
    )

    /** Cardinal/intercardinal name for a heading, used as an accessible description. */
    fun cardinalLabel(heading: Double, bangla: Boolean): String {
        val index = (((normalize(heading) + 22.5) % 360.0) / 45.0).toInt().coerceIn(0, 7)
        return if (bangla) CARDINALS_BN[index] else CARDINALS_EN[index]
    }

    /** Turns the raw heading into the sentence shown under the dial. */
    fun guidanceText(qiblaBearing: Double, heading: Double?, bangla: Boolean): String = when {
        heading == null -> if (bangla) "সেন্সরের জন্য অপেক্ষা করা হচ্ছে…" else "Waiting for the sensor…"
        isAligned(qiblaBearing, heading) ->
            if (bangla) "আপনি কিবলার দিকে আছেন" else "You are facing the Qibla"
        else -> {
            val delta = shortestDelta(heading, qiblaBearing)
            val direction = if (delta > 0) {
                if (bangla) "ডানদিকে" else "right"
            } else {
                if (bangla) "বামদিকে" else "left"
            }
            val degrees = abs(delta).toInt()
            if (bangla) "ফোনটি $degrees° $direction ঘুরান" else "Turn $degrees° $direction"
        }
    }
}
