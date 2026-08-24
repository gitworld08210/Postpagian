package com.pigeonpost.domain

import com.pigeonpost.data.model.Message
import com.pigeonpost.data.model.MessageStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for DeliverySimulator.
 *
 * Delivery is real time: a pigeon flies at 60 km/h and its state is a pure function of
 * the wall clock plus the fate the sender persisted in `death_at_progress`. These tests
 * feed an explicit `now` so they can inspect any point of a multi-hour journey.
 */
class DeliverySimulatorTest {

    private lateinit var calculator: PigeonDeliveryCalculator
    private lateinit var simulator: DeliverySimulator

    /** LA -> NYC: ~3936 km, so ~65.6 hours of real flight at 60 km/h. */
    private val sentAt = 1_700_000_000_000L

    @Before
    fun setup() {
        calculator = PigeonDeliveryCalculator()
        simulator = DeliverySimulator(calculator)
    }

    private fun message(
        id: String = "test-message",
        senderLat: Double = PigeonDeliveryCalculator.LA_LAT,
        senderLng: Double = PigeonDeliveryCalculator.LA_LNG,
        receiverLat: Double = PigeonDeliveryCalculator.NYC_LAT,
        receiverLng: Double = PigeonDeliveryCalculator.NYC_LNG,
        deathAtProgress: Double? = null
    ): Message {
        val distanceKm = calculator.calculateDistance(senderLat, senderLng, receiverLat, receiverLng)
        return Message(
            id = id,
            senderId = "sender",
            receiverId = "receiver",
            content = "Hello",
            sentAt = sentAt,
            deliveryTime = calculator.calculateDeliveryTimeMs(distanceKm),
            status = MessageStatus.FLYING,
            senderLat = senderLat,
            senderLng = senderLng,
            receiverLat = receiverLat,
            receiverLng = receiverLng,
            deathAtProgress = deathAtProgress
        )
    }

    // --- Fate assignment -----------------------------------------------------

    @Test
    fun `fate assignment dooms roughly twenty percent of pigeons`() {
        val trials = 20_000
        val random = Random(20240521)
        val doomed = (0 until trials).count { simulator.assignDeathProgress(random) != null }

        val rate = doomed.toDouble() / trials
        assertEquals(
            "Death rate should be close to ${PigeonDeliveryCalculator.DEATH_PROBABILITY}, was $rate",
            PigeonDeliveryCalculator.DEATH_PROBABILITY,
            rate,
            0.01
        )
    }

    @Test
    fun `surviving pigeon gets a null death point`() {
        val neverDieRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextDouble(): Double = 0.99 // above the 20% threshold
        }

        assertNull(simulator.assignDeathProgress(neverDieRandom))
    }

    @Test
    fun `doomed pigeon gets a death point inside the route`() {
        // First draw dooms the bird (below the 20% threshold), second picks where it falls.
        val scriptedRandom = scriptedRandom(0.01, 0.5)

        val deathAt = simulator.assignDeathProgress(scriptedRandom)

        assertNotNull(deathAt)
        assertTrue(
            "Death point $deathAt should sit within the route bounds",
            deathAt!! >= DeliverySimulator.MIN_DEATH_PROGRESS &&
                deathAt <= DeliverySimulator.MAX_DEATH_PROGRESS
        )
    }

    /** A Random that hands out the given doubles in order, repeating the last one. */
    private fun scriptedRandom(vararg values: Double): Random = object : Random() {
        private var index = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double {
            val value = values[index.coerceAtMost(values.lastIndex)]
            index++
            return value
        }
    }

    // --- Progress and position from the wall clock ---------------------------

    @Test
    fun `progress is elapsed wall clock time over delivery time`() {
        val message = message()
        val totalMs = simulator.totalDeliveryMs(message)

        assertEquals(0.0, simulator.snapshot(message, sentAt).progress, 0.0001)
        assertEquals(0.25, simulator.snapshot(message, sentAt + totalMs / 4).progress, 0.0001)
        assertEquals(0.5, simulator.snapshot(message, sentAt + totalMs / 2).progress, 0.0001)
    }

    @Test
    fun `delivery time matches distance at sixty kilometres per hour`() {
        val message = message()
        val distanceKm = calculator.calculateDistance(
            message.senderLat, message.senderLng,
            message.receiverLat, message.receiverLng
        )
        val expectedHours = distanceKm / PigeonDeliveryCalculator.PIGEON_SPEED_KMH

        val actualHours = simulator.totalDeliveryMs(message) / 3_600_000.0

        assertEquals(expectedHours, actualHours, 0.001)
        assertTrue("LA to NYC should take well over a day", actualHours > 24.0)
    }

    @Test
    fun `position is interpolated between sender and receiver at current progress`() {
        val message = message()
        val totalMs = simulator.totalDeliveryMs(message)

        val halfway = simulator.snapshot(message, sentAt + totalMs / 2)

        val expectedLat = (message.senderLat + message.receiverLat) / 2
        val expectedLng = (message.senderLng + message.receiverLng) / 2
        assertEquals(expectedLat, halfway.currentLat, 0.01)
        assertEquals(expectedLng, halfway.currentLng, 0.01)
    }

    @Test
    fun `position starts at the sender and ends at the receiver`() {
        val message = message()
        val totalMs = simulator.totalDeliveryMs(message)

        val start = simulator.snapshot(message, sentAt)
        assertEquals(message.senderLat, start.currentLat, 0.0001)
        assertEquals(message.senderLng, start.currentLng, 0.0001)

        val end = simulator.snapshot(message, sentAt + totalMs)
        assertEquals(message.receiverLat, end.currentLat, 0.0001)
        assertEquals(message.receiverLng, end.currentLng, 0.0001)
    }

    @Test
    fun `hours remaining reflects real remaining flight time`() {
        val message = message()
        val totalMs = simulator.totalDeliveryMs(message)
        val totalHours = totalMs / 3_600_000.0

        assertEquals(totalHours, simulator.hoursRemaining(message, sentAt), 0.01)
        assertEquals(totalHours / 2, simulator.hoursRemaining(message, sentAt + totalMs / 2), 0.01)
        assertEquals(0.0, simulator.hoursRemaining(message, sentAt + totalMs), 0.0001)
    }

    // --- Status derivation ---------------------------------------------------

    @Test
    fun `surviving pigeon is FLYING until the full duration has passed then DELIVERED`() {
        val message = message(deathAtProgress = null)
        val totalMs = simulator.totalDeliveryMs(message)

        assertEquals(MessageStatus.FLYING, simulator.snapshot(message, sentAt).status)
        assertEquals(MessageStatus.FLYING, simulator.snapshot(message, sentAt + totalMs / 2).status)
        assertEquals(MessageStatus.DELIVERED, simulator.snapshot(message, sentAt + totalMs).status)

        // Still delivered days later.
        val late = simulator.snapshot(message, sentAt + totalMs + 5 * 24 * 3_600_000L)
        assertEquals(MessageStatus.DELIVERED, late.status)
        assertEquals(1.0, late.progress, 0.0001)
    }

    @Test
    fun `doomed pigeon is FLYING before its death point and LOST at or after it`() {
        val deathAt = 0.4
        val message = message(deathAtProgress = deathAt)
        val totalMs = simulator.totalDeliveryMs(message)

        val before = simulator.snapshot(message, sentAt + (totalMs * (deathAt - 0.1)).toLong())
        assertEquals(MessageStatus.FLYING, before.status)

        val exactly = simulator.snapshot(message, sentAt + (totalMs * deathAt).toLong())
        assertEquals(MessageStatus.LOST, exactly.status)

        val after = simulator.snapshot(message, sentAt + (totalMs * (deathAt + 0.2)).toLong())
        assertEquals(MessageStatus.LOST, after.status)
    }

    @Test
    fun `doomed pigeon is frozen at its death point forever`() {
        val deathAt = 0.6
        val message = message(deathAtProgress = deathAt)
        val totalMs = simulator.totalDeliveryMs(message)

        val atDeath = simulator.snapshot(message, sentAt + (totalMs * deathAt).toLong())
        val muchLater = simulator.snapshot(message, sentAt + totalMs * 10)

        assertEquals(MessageStatus.LOST, muchLater.status)
        assertEquals(deathAt, muchLater.progress, 0.0001)
        assertEquals(atDeath.currentLat, muchLater.currentLat, 0.0001)
        assertEquals(atDeath.currentLng, muchLater.currentLng, 0.0001)

        // A doomed pigeon is never resurrected, even long past the full duration.
        assertTrue("Death position must be short of the destination", muchLater.progress < 1.0)
    }

    @Test
    fun `a doomed pigeon never reaches the destination`() {
        val message = message(deathAtProgress = 0.5)
        val totalMs = simulator.totalDeliveryMs(message)

        val update = simulator.snapshot(message, sentAt + totalMs + 1)

        assertEquals(MessageStatus.LOST, update.status)
        assertTrue(update.currentLat != message.receiverLat)
    }

    @Test
    fun `state is identical for every client reading the same clock`() {
        // Sender and recipient hold the same row, so both derive the same answer.
        val message = message(deathAtProgress = 0.33)
        val now = sentAt + simulator.totalDeliveryMs(message) / 2

        val senderView = simulator.snapshot(message, now)
        val recipientView = simulator.snapshot(message.copy(), now)

        assertEquals(senderView, recipientView)
    }

    @Test
    fun `same location message is delivered instantly`() {
        val message = message(
            senderLat = 34.0,
            senderLng = -118.0,
            receiverLat = 34.0,
            receiverLng = -118.0
        )

        val update = simulator.snapshot(message, sentAt)

        assertEquals(MessageStatus.DELIVERED, update.status)
        assertEquals(1.0, update.progress, 0.001)
    }

    @Test
    fun `progress never runs backwards or outside zero to one`() {
        val message = message()
        val totalMs = simulator.totalDeliveryMs(message)

        var previous = -1.0
        for (step in 0..20) {
            val update = simulator.snapshot(message, sentAt + (totalMs * step / 20))
            assertTrue("Progress ${update.progress} out of range", update.progress in 0.0..1.0)
            assertTrue("Progress should not decrease", update.progress >= previous)
            previous = update.progress
        }
    }

    @Test
    fun `clock skew before send time is treated as not yet departed`() {
        val message = message()

        val update = simulator.snapshot(message, sentAt - 60_000L)

        assertEquals(0.0, update.progress, 0.0001)
        assertEquals(MessageStatus.FLYING, update.status)
        assertEquals(message.senderLat, update.currentLat, 0.0001)
    }

    @Test
    fun `snapshot carries the message id and in-range coordinates`() {
        val messageId = "unique-message-id-123"
        val message = message(id = messageId)
        val totalMs = simulator.totalDeliveryMs(message)

        for (step in 0..10) {
            val update = simulator.snapshot(message, sentAt + (totalMs * step / 10))
            assertEquals(messageId, update.messageId)
            assertTrue(
                "Lat ${update.currentLat} should be between sender and receiver",
                update.currentLat in PigeonDeliveryCalculator.LA_LAT..PigeonDeliveryCalculator.NYC_LAT
            )
            assertTrue(
                "Lng ${update.currentLng} should be between sender and receiver",
                update.currentLng in PigeonDeliveryCalculator.LA_LNG..PigeonDeliveryCalculator.NYC_LNG
            )
        }
    }

    // --- The real-time ticker -----------------------------------------------

    @Test
    fun `tracking a finished flight emits once and stops`() = runTest {
        val message = message()
        val finished = sentAt + simulator.totalDeliveryMs(message)

        val updates = simulator.trackDelivery(message) { finished }.toList()

        assertEquals(1, updates.size)
        assertEquals(MessageStatus.DELIVERED, updates.single().status)
    }

    @Test
    fun `tracking a lost flight emits once and stops`() = runTest {
        val message = message(deathAtProgress = 0.2)
        val now = sentAt + (simulator.totalDeliveryMs(message) * 0.5).toLong()

        val updates = simulator.trackDelivery(message) { now }.toList()

        assertEquals(1, updates.size)
        assertEquals(MessageStatus.LOST, updates.single().status)
        assertEquals(0.2, updates.single().progress, 0.0001)
    }

    @Test
    fun `ticker re-reads the clock until the flight finishes`() = runTest {
        val message = message()
        val totalMs = simulator.totalDeliveryMs(message)

        // A clock that jumps a third of the journey on every read, so the ticker
        // observes FLYING, FLYING, then DELIVERED and completes.
        var reads = 0
        val jumpingClock = {
            val now = sentAt + (totalMs * reads) / 3
            reads++
            now
        }

        val updates = simulator.trackDelivery(message, jumpingClock).toList()

        assertEquals(4, updates.size)
        assertEquals(MessageStatus.FLYING, updates[0].status)
        assertEquals(MessageStatus.FLYING, updates[1].status)
        assertEquals(MessageStatus.FLYING, updates[2].status)
        assertEquals(MessageStatus.DELIVERED, updates[3].status)
        assertTrue(updates[1].progress > updates[0].progress)
    }
}
