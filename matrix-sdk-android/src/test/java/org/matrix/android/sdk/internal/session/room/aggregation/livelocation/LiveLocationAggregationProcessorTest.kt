/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.aggregation.livelocation

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.test.fakes.FakeClock
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.FakeWorkManagerProvider

private const val A_SESSION_ID = "session_id"
private const val A_SENDER_ID = "sender_id"
private const val AN_EVENT_ID = "event_id"

internal class LiveLocationAggregationProcessorTest {

    private val fakeWorkManagerProvider = FakeWorkManagerProvider()
    private val fakeClock = FakeClock()
    private val fakeRealm = FakeRealm()

    private val liveLocationAggregationProcessor = LiveLocationAggregationProcessor(
            sessionId = A_SESSION_ID,
            workManagerProvider = fakeWorkManagerProvider.instance,
            clock = fakeClock
    )

    @Test
    fun `given beacon info when it is local echo then it is ignored`() {
        val event = Event(senderId = A_SENDER_ID)
        val beaconInfo = MessageBeaconInfoContent()

        val result = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = event,
                content = beaconInfo,
                roomId = "",
                isLocalEcho = true
        )

        result shouldBeEqualTo false
    }

    @Test
    fun `given beacon info and event when senderId is null or empty then it is ignored`() {
        val eventNoSenderId = Event(eventId = AN_EVENT_ID)
        val eventEmptySenderId = Event(eventId = AN_EVENT_ID, senderId = "")
        val beaconInfo = MessageBeaconInfoContent()

        val resultNoSenderId = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = eventNoSenderId,
                content = beaconInfo,
                roomId = "",
                isLocalEcho = false
        )
        val resultEmptySenderId = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = eventEmptySenderId,
                content = beaconInfo,
                roomId = "",
                isLocalEcho = false
        )

        resultNoSenderId shouldBeEqualTo false
        resultEmptySenderId shouldBeEqualTo false
    }

    @Test
    fun `given beacon location data when it is local echo then it is ignored`() {
        val event = Event(senderId = A_SENDER_ID)
        val beaconLocationData = MessageBeaconLocationDataContent()

        val result = liveLocationAggregationProcessor.handleBeaconLocationData(
                realm = fakeRealm.instance,
                event = event,
                content = beaconLocationData,
                roomId = "",
                relatedEventId = "",
                isLocalEcho = true
        )

        result shouldBeEqualTo false
    }

    @Test
    fun `given beacon location data and event when senderId is null or empty then it is ignored`() {
        val eventNoSenderId = Event(eventId = AN_EVENT_ID)
        val eventEmptySenderId = Event(eventId = AN_EVENT_ID, senderId = "")
        val beaconLocationData = MessageBeaconLocationDataContent()

        val resultNoSenderId = liveLocationAggregationProcessor.handleBeaconLocationData(
                realm = fakeRealm.instance,
                event = eventNoSenderId,
                content = beaconLocationData,
                roomId = "",
                relatedEventId = "",
                isLocalEcho = false
        )
        val resultEmptySenderId = liveLocationAggregationProcessor.handleBeaconLocationData(
                realm = fakeRealm.instance,
                event = eventEmptySenderId,
                content = beaconLocationData,
                roomId = "",
                relatedEventId = "",
                isLocalEcho = false
        )

        resultNoSenderId shouldBeEqualTo false
        resultEmptySenderId shouldBeEqualTo false
    }
}
