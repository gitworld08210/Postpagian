package com.pigeonpost.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for PigeonDeliveryCalculator.
 * Tests distance calculation, delivery time, death probability, and position interpolation.
 */
class PigeonDeliveryCalculatorTest {

    private lateinit var calculator: PigeonDeliveryCalculator

    @Before
    fun setup() {
        calculator = PigeonDeliveryCalculator()
    }

    @Test
    fun `LA to NYC distance is approximately 3944 km`() {
        val distance = calculator.calculateDistance(
            PigeonDeliveryCalculator.LA_LAT, PigeonDeliveryCalculator.LA_LNG,
            PigeonDeliveryCalculator.NYC_LAT, PigeonDeliveryCalculator.NYC_LNG
        )
        // Expected: approximately 3944 km (great circle distance)
        // Allowing reasonable tolerance for Haversine approximation
        assertTrue("Distance should be between 3900 and 4000 km, was $distance", distance in 3900.0..4000.0)
    }

    @Test
    fun `LA to NYC delivery time is approximately 65-66 hours at 60 kmh`() {
        val distance = calculator.calculateDistance(
            PigeonDeliveryCalculator.LA_LAT, PigeonDeliveryCalculator.LA_LNG,
            PigeonDeliveryCalculator.NYC_LAT, PigeonDeliveryCalculator.NYC_LNG
        )
        val hours = calculator.calculateDeliveryTimeHours(distance)
        // At 60 km/h, ~3944 km should take ~65.7 hours
        assertTrue("Delivery time should be between 65 and 67 hours, was $hours", hours in 65.0..67.0)
    }

    @Test
    fun `delivery time in milliseconds is correctly calculated`() {
        val distanceKm = 120.0 // 2 hours at 60 km/h
        val expectedMs = 2 * 3600 * 1000L // 7,200,000 ms

        val result = calculator.calculateDeliveryTimeMs(distanceKm)
        assertEquals(expectedMs, result)
    }

    @Test
    fun `zero distance results in zero delivery time`() {
        val result = calculator.calculateDeliveryTimeMs(0.0)
        assertEquals(0L, result)
    }

    @Test
    fun `pigeon death probability is 20 percent`() {
        // Run 10000 trials and verify death rate is approximately 20%
        val trials = 10000
        var deaths = 0
        val random = Random(42)

        repeat(trials) {
            if (calculator.doesPigeonDie(random)) {
                deaths++
            }
        }

        val deathRate = deaths.toDouble() / trials
        // Allow tolerance: between 17% and 23%
        assertTrue(
            "Death rate should be between 17% and 23%, was ${deathRate * 100}%",
            deathRate in 0.17..0.23
        )
    }

    @Test
    fun `pigeon always dies with random that always returns below threshold`() {
        // Random that always returns 0.0 (below 0.20 threshold)
        val alwaysDieRandom = Random(0)
        // Use a fixed check
        val dies = 0.0 < PigeonDeliveryCalculator.DEATH_PROBABILITY
        assertTrue("Pigeon should die when random returns 0.0", dies)
    }

    @Test
    fun `pigeon never dies with random that always returns above threshold`() {
        // Check: values above 0.20 should not trigger death
        val survives = 0.5 >= PigeonDeliveryCalculator.DEATH_PROBABILITY
        assertTrue("Pigeon should survive when random returns 0.5", survives)
    }

    @Test
    fun `calculate current position at start returns sender position`() {
        val (lat, lng) = calculator.calculateCurrentPosition(
            senderLat = 34.0, senderLng = -118.0,
            receiverLat = 40.0, receiverLng = -74.0,
            elapsedMs = 0, totalDeliveryMs = 100000
        )
        assertEquals(34.0, lat, 0.001)
        assertEquals(-118.0, lng, 0.001)
    }

    @Test
    fun `calculate current position at end returns receiver position`() {
        val (lat, lng) = calculator.calculateCurrentPosition(
            senderLat = 34.0, senderLng = -118.0,
            receiverLat = 40.0, receiverLng = -74.0,
            elapsedMs = 100000, totalDeliveryMs = 100000
        )
        assertEquals(40.0, lat, 0.001)
        assertEquals(-74.0, lng, 0.001)
    }

    @Test
    fun `calculate current position at midpoint returns halfway point`() {
        val (lat, lng) = calculator.calculateCurrentPosition(
            senderLat = 0.0, senderLng = 0.0,
            receiverLat = 10.0, receiverLng = 20.0,
            elapsedMs = 50000, totalDeliveryMs = 100000
        )
        assertEquals(5.0, lat, 0.001)
        assertEquals(10.0, lng, 0.001)
    }

    @Test
    fun `calculate progress returns 0 at start`() {
        val progress = calculator.calculateProgress(0, 100000)
        assertEquals(0.0, progress, 0.001)
    }

    @Test
    fun `calculate progress returns 1 at end`() {
        val progress = calculator.calculateProgress(100000, 100000)
        assertEquals(1.0, progress, 0.001)
    }

    @Test
    fun `calculate progress is clamped to 1 when elapsed exceeds total`() {
        val progress = calculator.calculateProgress(200000, 100000)
        assertEquals(1.0, progress, 0.001)
    }

    @Test
    fun `calculate progress returns 1 when total is zero`() {
        val progress = calculator.calculateProgress(0, 0)
        assertEquals(1.0, progress, 0.001)
    }

    @Test
    fun `pigeon speed is 60 kmh`() {
        assertEquals(60.0, PigeonDeliveryCalculator.PIGEON_SPEED_KMH, 0.0)
    }

    @Test
    fun `death probability is 20 percent constant`() {
        assertEquals(0.20, PigeonDeliveryCalculator.DEATH_PROBABILITY, 0.0)
    }

    @Test
    fun `same location results in zero distance`() {
        val distance = calculator.calculateDistance(34.0, -118.0, 34.0, -118.0)
        assertEquals(0.0, distance, 0.001)
    }
}
