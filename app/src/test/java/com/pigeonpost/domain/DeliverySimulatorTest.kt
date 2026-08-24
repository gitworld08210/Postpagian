package com.pigeonpost.domain

import com.pigeonpost.data.model.Message
import com.pigeonpost.data.model.MessageStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for DeliverySimulator.
 * Tests position updates, delivery completion, and pigeon death mechanics.
 */
class DeliverySimulatorTest {

    private lateinit var calculator: PigeonDeliveryCalculator
    private lateinit var simulator: DeliverySimulator

    @Before
    fun setup() {
        calculator = PigeonDeliveryCalculator()
        simulator = DeliverySimulator(calculator)
    }

    @Test
    fun `same location message is delivered instantly`() = runTest {
        val message = Message(
            id = "test-1",
            senderId = "sender",
            receiverId = "receiver",
            content = "Hello",
            senderLat = 34.0,
            senderLng = -118.0,
            receiverLat = 34.0,
            receiverLng = -118.0
        )

        val updates = simulator.simulateDelivery(message, Random(42)).toList()

        assertEquals(1, updates.size)
        assertEquals(MessageStatus.DELIVERED, updates.last().status)
        assertEquals(1.0, updates.last().progress, 0.001)
    }

    @Test
    fun `delivery simulation emits updates with increasing progress`() = runTest {
        val message = Message(
            id = "test-2",
            senderId = "sender",
            receiverId = "receiver",
            content = "Hello",
            senderLat = 34.0,
            senderLng = -118.0,
            receiverLat = 34.5,
            receiverLng = -117.5  // Short distance for quick test
        )

        // Use random that never triggers death (value > 0.07)
        val neverDieRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextDouble(): Double = 0.99  // Always above death threshold
        }

        val updates = simulator.simulateDelivery(message, neverDieRandom).toList()

        assertTrue("Should have at least one update", updates.isNotEmpty())

        // Progress should be non-decreasing
        for (i in 1 until updates.size) {
            assertTrue(
                "Progress should not decrease: ${updates[i-1].progress} -> ${updates[i].progress}",
                updates[i].progress >= updates[i-1].progress
            )
        }

        // Final update should be DELIVERED
        assertEquals(MessageStatus.DELIVERED, updates.last().status)
    }

    @Test
    fun `pigeon death results in LOST status`() = runTest {
        val message = Message(
            id = "test-3",
            senderId = "sender",
            receiverId = "receiver",
            content = "Hello",
            senderLat = PigeonDeliveryCalculator.LA_LAT,
            senderLng = PigeonDeliveryCalculator.LA_LNG,
            receiverLat = PigeonDeliveryCalculator.NYC_LAT,
            receiverLng = PigeonDeliveryCalculator.NYC_LNG
        )

        // Use random that always triggers death on first check
        val alwaysDieRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextDouble(): Double = 0.0  // Always below any threshold
        }

        val updates = simulator.simulateDelivery(message, alwaysDieRandom).toList()

        assertTrue("Should have at least one update", updates.isNotEmpty())
        assertEquals(MessageStatus.LOST, updates.last().status)
        assertTrue("Progress should be less than 1.0", updates.last().progress < 1.0)
    }

    @Test
    fun `delivery update contains correct message id`() = runTest {
        val messageId = "unique-message-id-123"
        val message = Message(
            id = messageId,
            senderId = "sender",
            receiverId = "receiver",
            content = "Test",
            senderLat = 34.0,
            senderLng = -118.0,
            receiverLat = 34.0,
            receiverLng = -118.0
        )

        val updates = simulator.simulateDelivery(message, Random(42)).toList()

        updates.forEach { update ->
            assertEquals(messageId, update.messageId)
        }
    }

    @Test
    fun `delivery update coordinates stay within bounds`() = runTest {
        val message = Message(
            id = "test-bounds",
            senderId = "sender",
            receiverId = "receiver",
            content = "Hello",
            senderLat = 34.0,
            senderLng = -118.0,
            receiverLat = 40.0,
            receiverLng = -74.0
        )

        val neverDieRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextDouble(): Double = 0.99
        }

        val updates = simulator.simulateDelivery(message, neverDieRandom).toList()

        updates.forEach { update ->
            assertTrue(
                "Lat ${update.currentLat} should be between sender and receiver",
                update.currentLat in 34.0..40.0
            )
            assertTrue(
                "Lng ${update.currentLng} should be between sender and receiver",
                update.currentLng in -118.0..-74.0
            )
        }
    }
}
