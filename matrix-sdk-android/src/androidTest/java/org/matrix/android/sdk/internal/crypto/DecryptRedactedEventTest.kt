/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class DecryptRedactedEventTest : InstrumentedTest {

    @Test
    fun doNotFailToDecryptRedactedEvent() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val e2eRoomID = testData.roomId
        val aliceSession = testData.firstSession
        val bobSession = testData.secondSession!!

        val roomALicePOV = aliceSession.getRoom(e2eRoomID)!!
        val timelineEvent = testHelper.sendTextMessage(roomALicePOV, "Hello", 1).first()
        val redactionReason = "Wrong Room"
        roomALicePOV.sendService().redactEvent(timelineEvent.root, redactionReason)

        // get the event from bob
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                bobSession.getRoom(e2eRoomID)?.getTimelineEvent(timelineEvent.eventId)?.root?.isRedacted() == true
            }
        }

        val eventBobPov = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(timelineEvent.eventId)!!

        testHelper.runBlockingTest {
            try {
                val result = bobSession.cryptoService().decryptEvent(eventBobPov.root, "")
                Assert.assertEquals(
                        "Unexpected redacted reason",
                        redactionReason,
                        result.clearEvent.toModel<Event>()?.unsignedData?.redactedEvent?.content?.get("reason")
                )
                Assert.assertEquals(
                        "Unexpected Redacted event id",
                        timelineEvent.eventId,
                        result.clearEvent.toModel<Event>()?.unsignedData?.redactedEvent?.redacts
                )
            } catch (failure: Throwable) {
                Assert.fail("Should not throw when decrypting a redacted event")
            }
        }
    }
}
