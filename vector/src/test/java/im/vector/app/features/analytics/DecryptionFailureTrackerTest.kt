/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.plan.Error
import im.vector.app.test.fakes.FakeActiveSessionDataSource
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeClock
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.shared.createTimberTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.text.SimpleDateFormat

@ExperimentalCoroutinesApi
class DecryptionFailureTrackerTest {

    @Rule
    fun timberTestRule() = createTimberTestRule()

    private val fakeAnalyticsTracker = FakeAnalyticsTracker()

    private val fakeActiveSessionDataSource = FakeActiveSessionDataSource()

    private val fakeClock = FakeClock()

    val reportedEvents = mutableSetOf<String>()

    private val fakePersistence = mockk<ReportedDecryptionFailurePersistence> {

        coEvery { load() } just runs
        coEvery { persist() } just runs
        coEvery { markAsReported(any()) } coAnswers  {
            reportedEvents.add(firstArg())
        }
        every { hasBeenReported(any()) } answers  {
            reportedEvents.contains(firstArg())
        }
    }

    private val decryptionFailureTracker = DecryptionFailureTracker(
            fakeAnalyticsTracker,
            fakeActiveSessionDataSource.instance,
            fakePersistence,
            fakeClock
    )

    private val aCredential = Credentials(
                    userId = "@alice:matrix.org",
                    deviceId = "ABCDEFGHT",
                    homeServer = "http://matrix.org",
                    accessToken = "qwerty",
                    refreshToken = null,
            )

    private val fakeMxOrgTestSession = FakeSession().apply {
        givenSessionParams(
                SessionParams(
                        credentials = aCredential,
                        homeServerConnectionConfig = mockk(relaxed = true),
                        isTokenValid = true,
                        loginType = LoginType.PASSWORD
                )
        )
        fakeUserId = "@alice:matrix.org"
    }

    private val aUISIError = MXCryptoError.Base(
            MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID,
            "",
            detailedErrorDescription = ""
    )

    private val aFakeBobMxOrgEvent = Event(
            originServerTs = 90_000,
            eventId = "$000",
            senderId = "@bob:matrix.org",
            roomId = "!roomA"
    )

    @Before
    fun setupTest() {
        reportedEvents.clear()
        fakeMxOrgTestSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(false)
    }

    @Test
    fun `should report late decryption to analytics tracker`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        every {
            fakeAnalyticsTracker.capture(any())
        } just runs

        fakeClock.givenEpoch(100_000)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        fakeSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(true)

        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()
        // advance time by 5 seconds
        fakeClock.givenEpoch(105_000)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        // it should report
        verify(exactly = 1) { fakeAnalyticsTracker.capture(any()) }

        verify {
            fakeAnalyticsTracker.capture(
                    im.vector.app.features.analytics.plan.Error(
                            "mxc_crypto_error_type|",
                            cryptoModule = Error.CryptoModule.Rust,
                            domain = Error.Domain.E2EE,
                            name = Error.Name.OlmKeysNotSentError,
                            cryptoSDK = Error.CryptoSDK.Rust,
                            timeToDecryptMillis = 5000,
                            isFederated = false,
                            isMatrixDotOrg = true,
                            userTrustsOwnIdentity = true,
                            wasVisibleToUser = false
                    ),
            )
        }

        // Can't do that in @Before function, it wont work as test will fail with:
        // "the test coroutine is not completing, there were active child jobs"
        // as the decryptionFailureTracker is setup to use the current test coroutine scope (?)
        decryptionFailureTracker.stop()
    }

    @Test
    fun `should not report graced late decryption to analytics tracker`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)

        runCurrent()
        // advance time by 3 seconds
        currentFakeTime += 3_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(
                event,
                emptyMap()
        )

        runCurrent()

        // it should not have reported it
        verify(exactly = 0) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report time to decrypt for late decryption`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        fakeSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(true)

        val event = aFakeBobMxOrgEvent
        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)

        runCurrent()
        // advance time by 7 seconds, to be ahead of the 3 seconds grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(
                event,
                emptyMap()
        )

        runCurrent()

        // it should report
        verify(exactly = 1) { fakeAnalyticsTracker.capture(any()) }

        val error = eventSlot.captured as Error
        error.timeToDecryptMillis shouldBeEqualTo 7000

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report isMatrixDotOrg`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        val error = eventSlot.captured as Error
        error.isMatrixDotOrg shouldBeEqualTo true

        val otherSession = FakeSession().apply {
            givenSessionParams(
                    SessionParams(
                            credentials = aCredential.copy(userId = "@alice:another.org"),
                            homeServerConnectionConfig = mockk(relaxed = true),
                            isTokenValid = true,
                            loginType = LoginType.PASSWORD
                    )
            )
            every { sessionId } returns "WWEERE"
            fakeUserId = "@alice:another.org"
            this.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(true)
        }
        fakeActiveSessionDataSource.setActiveSession(otherSession)
        runCurrent()

        val event2 = aFakeBobMxOrgEvent.copy(eventId = "$001")

        decryptionFailureTracker.onEventDecryptionError(event2, aUISIError)
        runCurrent()

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(event2, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).isMatrixDotOrg shouldBeEqualTo false

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if user trusted it's identity at time of decryption`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        fakeSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(false)
        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        fakeSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(true)
        val event2 = aFakeBobMxOrgEvent.copy(eventId = "$001")
        decryptionFailureTracker.onEventDecryptionError(event2, aUISIError)
        runCurrent()

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).userTrustsOwnIdentity shouldBeEqualTo false

        decryptionFailureTracker.onEventDecrypted(event2, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).userTrustsOwnIdentity shouldBeEqualTo true

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should not report same event twice`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        every {
            fakeAnalyticsTracker.capture(any())
        } just runs

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)

        runCurrent()

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        verify(exactly = 1) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        verify(exactly = 1) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if isFedrated`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        val event2 = aFakeBobMxOrgEvent.copy(
                eventId = "$001",
                senderId = "@bob:another.org",
        )
        decryptionFailureTracker.onEventDecryptionError(event2, aUISIError)
        runCurrent()

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).isFederated shouldBeEqualTo false

        decryptionFailureTracker.onEventDecrypted(event2, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).isFederated shouldBeEqualTo true

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if wasVisibleToUser`() = runTest {
        val fakeSession = fakeMxOrgTestSession
        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs
        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        val event2 = aFakeBobMxOrgEvent.copy(
                eventId = "$001",
                senderId = "@bob:another.org",
        )
        decryptionFailureTracker.onEventDecryptionError(event2, aUISIError)
        runCurrent()

        decryptionFailureTracker.utdDisplayedInTimeline(
                mockk<TimelineEvent>(relaxed = true).apply {
                    every { root } returns event2
                    every { eventId } returns event2.eventId.orEmpty()
                }
        )

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate it's decrypted
        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).wasVisibleToUser shouldBeEqualTo false

        decryptionFailureTracker.onEventDecrypted(event2, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).wasVisibleToUser shouldBeEqualTo true

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if event relative age to session`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val historicalEventTimestamp = formatter.parse("2024-03-08 09:24:11")!!.time
        val sessionCreationTime = formatter.parse("2024-03-09 10:00:00")!!.time
        // 1mn after creation
        val liveEventTimestamp = formatter.parse("2024-03-09 10:01:00")!!.time

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        fakeSession.fakeCryptoService.cryptoDeviceInfo = CryptoDeviceInfo(
                deviceId = "ABCDEFGHT",
                userId = "@alice:matrix.org",
                firstTimeSeenLocalTs = sessionCreationTime
        )

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val event = aFakeBobMxOrgEvent.copy(
                originServerTs = historicalEventTimestamp
        )

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        val liveEvent = aFakeBobMxOrgEvent.copy(
                eventId = "$001",
                originServerTs = liveEventTimestamp
        )
        decryptionFailureTracker.onEventDecryptionError(liveEvent, aUISIError)
        runCurrent()

        // advance time by 7 seconds, to be ahead of the grace period
        currentFakeTime += 7_000
        fakeClock.givenEpoch(currentFakeTime)

        // Now simulate historical event late decrypt
        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).eventLocalAgeMillis shouldBeEqualTo (historicalEventTimestamp - sessionCreationTime).toInt()

        decryptionFailureTracker.onEventDecrypted(liveEvent, emptyMap())
        runCurrent()

        (eventSlot.captured as Error).eventLocalAgeMillis shouldBeEqualTo (liveEventTimestamp - sessionCreationTime).toInt()
        (eventSlot.captured as Error).eventLocalAgeMillis shouldBeEqualTo 60 * 1000

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report historical UTDs as an expected UTD if not verified`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val historicalEventTimestamp = formatter.parse("2024-03-08 09:24:11")!!.time
        val sessionCreationTime = formatter.parse("2024-03-09 10:00:00")!!.time

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        fakeSession.fakeCryptoService.cryptoDeviceInfo = CryptoDeviceInfo(
                deviceId = "ABCDEFGHT",
                userId = "@alice:matrix.org",
                firstTimeSeenLocalTs = sessionCreationTime
        )

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        // historical event and session not verified
        fakeSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(false)
        val event = aFakeBobMxOrgEvent.copy(
                originServerTs = historicalEventTimestamp
        )
        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        // advance time to be ahead of the permanent UTD period
        currentFakeTime += 70_000
        fakeClock.givenEpoch(currentFakeTime)
        advanceTimeBy(70_000)
        runCurrent()

        (eventSlot.captured as Error).name shouldBeEqualTo Error.Name.HistoricalMessage

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should not report historical UTDs as an expected UTD if verified`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val historicalEventTimestamp = formatter.parse("2024-03-08 09:24:11")!!.time
        val sessionCreationTime = formatter.parse("2024-03-09 10:00:00")!!.time

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        fakeSession.fakeCryptoService.cryptoDeviceInfo = CryptoDeviceInfo(
                deviceId = "ABCDEFGHT",
                userId = "@alice:matrix.org",
                firstTimeSeenLocalTs = sessionCreationTime
        )

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        // historical event and session not verified
        fakeSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(true)
        val event = aFakeBobMxOrgEvent.copy(
                originServerTs = historicalEventTimestamp
        )
        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        // advance time to be ahead of the permanent UTD period
        currentFakeTime += 70_000
        fakeClock.givenEpoch(currentFakeTime)
        advanceTimeBy(70_000)
        runCurrent()

        (eventSlot.captured as Error).name shouldNotBeEqualTo Error.Name.HistoricalMessage

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should not report live UTDs as an expected UTD even if not verified`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val sessionCreationTime = formatter.parse("2024-03-09 10:00:00")!!.time
        // 1mn after creation
        val liveEventTimestamp = formatter.parse("2024-03-09 10:01:00")!!.time

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        fakeSession.fakeCryptoService.cryptoDeviceInfo = CryptoDeviceInfo(
                deviceId = "ABCDEFGHT",
                userId = "@alice:matrix.org",
                firstTimeSeenLocalTs = sessionCreationTime
        )

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        // historical event and session not verified
        fakeSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(false)
        val event = aFakeBobMxOrgEvent.copy(
                originServerTs = liveEventTimestamp
        )
        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        // advance time to be ahead of the permanent UTD period
        currentFakeTime += 70_000
        fakeClock.givenEpoch(currentFakeTime)
        advanceTimeBy(70_000)
        runCurrent()

        (eventSlot.captured as Error).name shouldNotBeEqualTo Error.Name.HistoricalMessage

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if permanent UTD`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val eventSlot = slot<VectorAnalyticsEvent>()

        every {
            fakeAnalyticsTracker.capture(event = capture(eventSlot))
        } just runs

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val event = aFakeBobMxOrgEvent

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)
        runCurrent()

        currentFakeTime += 70_000
        fakeClock.givenEpoch(currentFakeTime)
        advanceTimeBy(70_000)
        runCurrent()

        verify(exactly = 1) { fakeAnalyticsTracker.capture(any()) }

        (eventSlot.captured as Error).timeToDecryptMillis shouldBeEqualTo -1
        decryptionFailureTracker.stop()
    }

    @Test
    fun `with multiple UTD`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        every {
            fakeAnalyticsTracker.capture(any())
        } just runs

        var currentFakeTime = 100_000L
        fakeClock.givenEpoch(currentFakeTime)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        decryptionFailureTracker.start(CoroutineScope(coroutineContext))
        runCurrent()

        val events = (0..10).map {
            aFakeBobMxOrgEvent.copy(
                    eventId = "000$it",
                    originServerTs = 50_000 + it * 1000L
            )
        }

        events.forEach {
            decryptionFailureTracker.onEventDecryptionError(it, aUISIError)
        }
        runCurrent()

        currentFakeTime += 70_000
        fakeClock.givenEpoch(currentFakeTime)
        advanceTimeBy(70_000)
        runCurrent()

        verify(exactly = 11) { fakeAnalyticsTracker.capture(any()) }

        decryptionFailureTracker.stop()
    }
}
