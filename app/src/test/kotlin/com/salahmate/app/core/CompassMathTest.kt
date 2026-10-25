package com.salahmate.app.core

import com.salahmate.app.core.sensors.CompassAccuracy
import com.salahmate.app.core.sensors.CompassMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compass maths used by the Qibla screen.
 *
 * The two reference bearings are the Adhan library's great-circle results recorded earlier for this
 * project: Dhaka → Makkah 277.57°, London → Makkah 118.99°.
 */
class CompassMathTest {

    private val dhakaQibla = 277.57
    private val londonQibla = 118.99

    @Test
    fun `normalize wraps negative and large angles`() {
        assertEquals(0.0, CompassMath.normalize(0.0), 0.001)
        assertEquals(350.0, CompassMath.normalize(-10.0), 0.001)
        assertEquals(10.0, CompassMath.normalize(370.0), 0.001)
        assertEquals(180.0, CompassMath.normalize(180.0), 0.001)
        assertEquals(359.0, CompassMath.normalize(-361.0), 0.001)
    }

    @Test
    fun `shortest delta never exceeds half a turn`() {
        assertEquals(2.0, CompassMath.shortestDelta(359.0, 1.0), 0.001)
        assertEquals(-2.0, CompassMath.shortestDelta(1.0, 359.0), 0.001)
        assertEquals(90.0, CompassMath.shortestDelta(0.0, 90.0), 0.001)
        assertEquals(-90.0, CompassMath.shortestDelta(90.0, 0.0), 0.001)
        assertEquals(180.0, Math.abs(CompassMath.shortestDelta(0.0, 180.0)), 0.001)
    }

    @Test
    fun `angular distance is symmetric and bounded`() {
        assertEquals(
            CompassMath.angularDistance(dhakaQibla, 10.0),
            CompassMath.angularDistance(10.0, dhakaQibla),
            0.001,
        )
        listOf(0.0, 45.0, 90.0, 180.0, 270.0, 359.0).forEach { a ->
            listOf(0.0, 12.0, 200.0, 350.0).forEach { b ->
                val d = CompassMath.angularDistance(a, b)
                assertTrue("distance $d between $a and $b", d >= 0.0 && d <= 180.0)
            }
        }
    }

    @Test
    fun `arrow rotation points at the qibla for the current heading`() {
        // Facing north in Dhaka: the arrow must swing to 277.57°.
        assertEquals(dhakaQibla, CompassMath.arrowRotation(dhakaQibla, 0.0), 0.01)
        // Facing the qibla already: the arrow points straight up.
        assertEquals(0.0, CompassMath.arrowRotation(dhakaQibla, dhakaQibla), 0.01)
        // Facing east: arrow is 187.57° from the top of the dial.
        assertEquals(187.57, CompassMath.arrowRotation(dhakaQibla, 90.0), 0.01)
        // No sensor reading yet: falls back to true north without crashing.
        assertEquals(dhakaQibla, CompassMath.arrowRotation(dhakaQibla, null), 0.01)
    }

    @Test
    fun `alignment uses the shortest arc`() {
        assertTrue(CompassMath.isAligned(londonQibla, 118.99))
        assertTrue(CompassMath.isAligned(londonQibla, 121.0))   // 2° off
        assertTrue(CompassMath.isAligned(londonQibla, 116.0))   // 3° off
        assertFalse(CompassMath.isAligned(londonQibla, 130.0))  // 11° off
        // 3° across the 0/360 boundary must count as aligned, not 357° off.
        assertTrue(CompassMath.isAligned(1.0, 359.0, toleranceDegrees = 6.0))
    }

    @Test
    fun `smoothing follows the shortest arc across north`() {
        // 350° -> 10° should move through 0°, not backwards through 180°.
        val step = CompassMath.smooth(350.0, 10.0, factor = 0.5)
        assertEquals(0.0, step, 0.001)
        // First reading is taken as-is.
        assertEquals(123.0, CompassMath.smooth(null, 123.0), 0.001)
        // Repeated smoothing converges on the target.
        var heading = 0.0
        repeat(100) { heading = CompassMath.smooth(heading, 90.0, factor = 0.3) }
        assertEquals(90.0, heading, 0.5)
        // A factor of 1 jumps straight to the new reading.
        assertEquals(200.0, CompassMath.smooth(10.0, 200.0, factor = 1.0), 0.001)
    }

    @Test
    fun `sensor accuracy maps to a described level`() {
        assertEquals(CompassAccuracy.UNRELIABLE, CompassMath.accuracyFrom(0))
        assertEquals(CompassAccuracy.LOW, CompassMath.accuracyFrom(1))
        assertEquals(CompassAccuracy.MEDIUM, CompassMath.accuracyFrom(2))
        assertEquals(CompassAccuracy.HIGH, CompassMath.accuracyFrom(3))
    }

    @Test
    fun `cardinal labels are localised at every boundary`() {
        assertEquals("N", CompassMath.cardinalLabel(0.0, bangla = false))
        assertEquals("NE", CompassMath.cardinalLabel(45.0, bangla = false))
        assertEquals("E", CompassMath.cardinalLabel(90.0, bangla = false))
        assertEquals("S", CompassMath.cardinalLabel(180.0, bangla = false))
        assertEquals("W", CompassMath.cardinalLabel(270.0, bangla = false))
        assertEquals("N", CompassMath.cardinalLabel(350.0, bangla = false))
        assertEquals("N", CompassMath.cardinalLabel(22.0, bangla = false))
        assertEquals("NE", CompassMath.cardinalLabel(23.0, bangla = false))
        assertEquals("উত্তর", CompassMath.cardinalLabel(0.0, bangla = true))
        assertEquals("দক্ষিণ", CompassMath.cardinalLabel(180.0, bangla = true))
    }

    @Test
    fun `guidance text tells the user which way to turn`() {
        assertEquals(
            "You are facing the Qibla",
            CompassMath.guidanceText(dhakaQibla, 277.0, bangla = false),
        )
        assertTrue(CompassMath.guidanceText(dhakaQibla, 200.0, bangla = false).contains("right"))
        assertTrue(CompassMath.guidanceText(dhakaQibla, 350.0, bangla = false).contains("left"))
        assertEquals("Waiting for the sensor…", CompassMath.guidanceText(dhakaQibla, null, bangla = false))
        assertTrue(CompassMath.guidanceText(dhakaQibla, 200.0, bangla = true).contains("ডানদিকে"))
    }
}
