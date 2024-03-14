/*
 * Copyright (c) 2024 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.analytics

import im.vector.app.features.analytics.plan.Error
import im.vector.app.test.fakes.FakeActiveSessionDataSource
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeClock
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.shared.createTimberTestRule
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

    private val decryptionFailureTracker = DecryptionFailureTracker(
            fakeAnalyticsTracker,
            fakeActiveSessionDataSource.instance,
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
        fakeMxOrgTestSession.fakeCryptoService.fakeCrossSigningService.givenIsCrossSigningVerifiedReturns(false)
    }

    @Test
    fun `should report late decryption to analytics tracker`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        every {
            fakeAnalyticsTracker.capture(any(), any())
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
        verify(exactly = 1) { fakeAnalyticsTracker.capture(any(), any()) }

        verify {
            fakeAnalyticsTracker.capture(
                    Error(
                            "mxc_crypto_error_type|",
                            Error.CryptoModule.Rust,
                            Error.Domain.E2EE,
                            Error.Name.OlmKeysNotSentError
                    ),
                    any()
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
        verify(exactly = 0) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report time to decrypt for late decryption`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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
        verify(exactly = 1) { fakeAnalyticsTracker.capture(any(), any()) }

        val properties = propertiesSlot.captured
        properties["timeToDecryptMillis"] shouldBeEqualTo 7000L

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report isMatrixDotOrg`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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

        propertiesSlot.captured["isMatrixDotOrg"] shouldBeEqualTo true

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

        propertiesSlot.captured["isMatrixDotOrg"] shouldBeEqualTo false

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if user trusted it's identity at time of decryption`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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

        propertiesSlot.captured["userTrustsOwnIdentity"] shouldBeEqualTo false

        decryptionFailureTracker.onEventDecrypted(event2, emptyMap())
        runCurrent()

        propertiesSlot.captured["userTrustsOwnIdentity"] shouldBeEqualTo true

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should not report same event twice`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        every {
            fakeAnalyticsTracker.capture(any(), any())
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

        verify(exactly = 1) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.onEventDecryptionError(event, aUISIError)

        runCurrent()

        decryptionFailureTracker.onEventDecrypted(event, emptyMap())
        runCurrent()

        verify(exactly = 1) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if isFedrated`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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

        propertiesSlot.captured["isFederated"] shouldBeEqualTo false

        decryptionFailureTracker.onEventDecrypted(event2, emptyMap())
        runCurrent()

        propertiesSlot.captured["isFederated"] shouldBeEqualTo true

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if wasVisibleToUser`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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

        propertiesSlot.captured["wasVisibleToUser"] shouldBeEqualTo false

        decryptionFailureTracker.onEventDecrypted(event2, emptyMap())
        runCurrent()

        propertiesSlot.captured["wasVisibleToUser"] shouldBeEqualTo true

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if event relative age to session`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val historicalEventTimestamp = formatter.parse("2024-03-08 09:24:11")!!.time
        val sessionCreationTime = formatter.parse("2024-03-09 10:00:00")!!.time
        // 1mn after creation
        val liveEventTimestamp = formatter.parse("2024-03-09 10:01:00")!!.time

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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

        propertiesSlot.captured["eventLocalAgeAtDecryptionFailure"] shouldBeEqualTo (historicalEventTimestamp - sessionCreationTime)

        decryptionFailureTracker.onEventDecrypted(liveEvent, emptyMap())
        runCurrent()

        propertiesSlot.captured["eventLocalAgeAtDecryptionFailure"] shouldBeEqualTo (liveEventTimestamp - sessionCreationTime)
        propertiesSlot.captured["eventLocalAgeAtDecryptionFailure"] shouldBeEqualTo 60 * 1000L

        verify(exactly = 2) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.stop()
    }

    @Test
    fun `should report if permanent UTD`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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

        verify(exactly = 1) { fakeAnalyticsTracker.capture(any(), any()) }

        propertiesSlot.captured["timeToDecryptMillis"] shouldBeEqualTo -1L
        decryptionFailureTracker.stop()
    }

    @Test
    fun `with multiple UTD`() = runTest {
        val fakeSession = fakeMxOrgTestSession

        val propertiesSlot = slot<Map<String, Any>>()

        every {
            fakeAnalyticsTracker.capture(any(), customProperties = capture(propertiesSlot))
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

        verify(exactly = 11) { fakeAnalyticsTracker.capture(any(), any()) }

        decryptionFailureTracker.stop()
    }
}
