/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto

import im.vector.app.E2EMessageDetected
import im.vector.app.UISIDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.fail
import org.junit.Assert
import org.junit.Test
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toContent

class UISIDetectorTest {

    @Test
    fun `trigger detection after grace period`() {
        val gracePeriod = 5_000L
        val uisiDetector = UISIDetector(gracePeriod)
        var detectedEvent: E2EMessageDetected? = null

        uisiDetector.callback = object : UISIDetector.UISIDetectorCallback {
            override val enabled = true
            override val reciprocateToDeviceEventType = "foo"

            override fun uisiDetected(source: E2EMessageDetected) {
                detectedEvent = source
            }

            override fun uisiReciprocateRequest(source: Event) {
                // nop
            }
        }

        // report a decryption error
        val eventId = "0001"
        val event = fakeEncryptedEvent(eventId, "s1", "r1")
        uisiDetector.onEventDecryptionError(event, fakeCryptoError())

        runBlocking {
            delay((gracePeriod * 1.2).toLong())
        }
        Assert.assertEquals(eventId, detectedEvent?.eventId)
    }

    @Test
    fun `If event decrypted during grace period should not trigger detection`() {
        val scope = CoroutineScope(SupervisorJob())
        val gracePeriod = 5_000L
        val uisiDetector = UISIDetector(gracePeriod)

        uisiDetector.callback = object : UISIDetector.UISIDetectorCallback {
            override val enabled = true
            override val reciprocateToDeviceEventType = "foo"

            override fun uisiDetected(source: E2EMessageDetected) {
                fail("Shouldn't trigger")
            }

            override fun uisiReciprocateRequest(source: Event) {
                // nop
            }
        }

        // report a decryption error
        val event = fakeEncryptedEvent("0001", "s1", "r1")
        uisiDetector.onEventDecryptionError(event, fakeCryptoError())

        // the grace period is 30s
        scope.launch(Dispatchers.Default) {
            delay((gracePeriod * 0.5).toLong())
            uisiDetector.onEventDecrypted(event, emptyMap())
        }

        runBlocking {
            delay((gracePeriod * 1.2).toLong())
        }
    }

    private fun fakeEncryptedEvent(eventId: String, sessionId: String, roomId: String): Event {
        return Event(
                type = EventType.ENCRYPTED,
                eventId = eventId,
                roomId = roomId,
                content = EncryptedEventContent(
                        algorithm = MXCRYPTO_ALGORITHM_MEGOLM,
                        ciphertext = "AwgBEpACQEKOkd4Gp0+gSXG4M+btcrnPgsF23xs/lUmS2I4YjmqF...",
                        sessionId = sessionId,
                        senderKey = "5e3EIqg3JfooZnLQ2qHIcBarbassQ4qXblai0",
                        deviceId = "FAKEE"
                ).toContent()
        )
    }

    private fun fakeCryptoError(error: MXCryptoError.ErrorType = MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) = MXCryptoError.Base(
            error,
            "A description",
            "Human readable"
    )
}
