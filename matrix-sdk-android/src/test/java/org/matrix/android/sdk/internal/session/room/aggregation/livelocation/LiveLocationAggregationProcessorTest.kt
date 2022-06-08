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

import androidx.work.OneTimeWorkRequest
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntityFields
import org.matrix.android.sdk.test.fakes.FakeClock
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.FakeWorkManagerProvider
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindAll
import org.matrix.android.sdk.test.fakes.givenFindFirst
import org.matrix.android.sdk.test.fakes.givenNotEqualTo

private const val A_SESSION_ID = "session_id"
private const val A_SENDER_ID = "sender_id"
private const val AN_EVENT_ID = "event_id"
private const val A_ROOM_ID = "room_id"
private const val A_TIMESTAMP = 1654689143L
private const val A_TIMEOUT_MILLIS = 15 * 60 * 1000L

internal class LiveLocationAggregationProcessorTest {

    private val fakeWorkManagerProvider = FakeWorkManagerProvider()
    private val fakeClock = FakeClock()
    private val fakeRealm = FakeRealm()
    private val fakeQuery = fakeRealm.givenWhere<LiveLocationShareAggregatedSummaryEntity>()

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
                roomId = A_ROOM_ID,
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
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )
        val resultEmptySenderId = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = eventEmptySenderId,
                content = beaconInfo,
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )

        resultNoSenderId shouldBeEqualTo false
        resultEmptySenderId shouldBeEqualTo false
    }

    @Test
    fun `given beacon info when no target eventId is found then it is ignored`() {
        val unsignedDataWithNoEventId = UnsignedData(
                age = 123
        )
        val unsignedDataWithEmptyEventId = UnsignedData(
                age = 123,
                replacesState = ""
        )
        val eventWithNoEventId = Event(senderId = A_SENDER_ID, unsignedData = unsignedDataWithNoEventId)
        val eventWithEmptyEventId = Event(senderId = A_SENDER_ID, eventId = "", unsignedData = unsignedDataWithEmptyEventId)
        val beaconInfoLive = MessageBeaconInfoContent(isLive = true)
        val beaconInfoNotLive = MessageBeaconInfoContent(isLive = false)

        val resultLiveNoEventId = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = eventWithNoEventId,
                content = beaconInfoLive,
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )
        val resultLiveEmptyEventId = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = eventWithEmptyEventId,
                content = beaconInfoLive,
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )
        val resultNotLiveNoEventId = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = eventWithNoEventId,
                content = beaconInfoNotLive,
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )
        val resultNotLiveEmptyEventId = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = eventWithEmptyEventId,
                content = beaconInfoNotLive,
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )

        resultLiveNoEventId shouldBeEqualTo false
        resultLiveEmptyEventId shouldBeEqualTo false
        resultNotLiveNoEventId shouldBeEqualTo false
        resultNotLiveEmptyEventId shouldBeEqualTo false
    }

    @Test
    fun `given beacon info and existing entity when beacon content is correct and active then it is aggregated`() {
        val event = Event(
                senderId = A_SENDER_ID,
                eventId = AN_EVENT_ID
        )
        val beaconInfo = MessageBeaconInfoContent(
                isLive = true,
                unstableTimestampMillis = A_TIMESTAMP,
                timeout = A_TIMEOUT_MILLIS
        )
        fakeClock.givenEpoch(A_TIMESTAMP + 5000)
        val aggregatedEntity = mockLiveLocationShareAggregatedSummaryEntityForEvent()
        val previousEntities = mockPreviousLiveLocationShareAggregatedSummaryEntities()

        val result = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = event,
                content = beaconInfo,
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )

        result shouldBeEqualTo true
        aggregatedEntity.eventId shouldBeEqualTo AN_EVENT_ID
        aggregatedEntity.roomId shouldBeEqualTo A_ROOM_ID
        aggregatedEntity.userId shouldBeEqualTo A_SENDER_ID
        aggregatedEntity.isActive shouldBeEqualTo true
        aggregatedEntity.endOfLiveTimestampMillis shouldBeEqualTo A_TIMESTAMP + A_TIMEOUT_MILLIS
        aggregatedEntity.lastLocationContent shouldBeEqualTo null
        previousEntities.forEach { entity ->
            entity.isActive shouldBeEqualTo false
        }
        val workManager = fakeWorkManagerProvider.instance.workManager
        verify { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `given beacon info and existing entity when beacon content is correct and inactive then it is aggregated`() {
        val unsignedData = UnsignedData(
                age = 123,
                replacesState = AN_EVENT_ID
        )
        val event = Event(
                senderId = A_SENDER_ID,
                eventId = "",
                unsignedData = unsignedData
        )
        val beaconInfo = MessageBeaconInfoContent(
                isLive = false,
                unstableTimestampMillis = A_TIMESTAMP,
                timeout = A_TIMEOUT_MILLIS
        )
        fakeClock.givenEpoch(A_TIMESTAMP + 5000)
        val aggregatedEntity = mockLiveLocationShareAggregatedSummaryEntityForEvent()
        val previousEntities = mockPreviousLiveLocationShareAggregatedSummaryEntities()

        val result = liveLocationAggregationProcessor.handleBeaconInfo(
                realm = fakeRealm.instance,
                event = event,
                content = beaconInfo,
                roomId = A_ROOM_ID,
                isLocalEcho = false
        )

        result shouldBeEqualTo true
        aggregatedEntity.eventId shouldBeEqualTo AN_EVENT_ID
        aggregatedEntity.roomId shouldBeEqualTo A_ROOM_ID
        aggregatedEntity.userId shouldBeEqualTo A_SENDER_ID
        aggregatedEntity.isActive shouldBeEqualTo false
        aggregatedEntity.endOfLiveTimestampMillis shouldBeEqualTo A_TIMESTAMP + A_TIMEOUT_MILLIS
        aggregatedEntity.lastLocationContent shouldBeEqualTo null
        previousEntities.forEach { entity ->
            entity.isActive shouldBeEqualTo false
        }
        val workManager = fakeWorkManagerProvider.instance.workManager
        verify { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `given beacon location data when it is local echo then it is ignored`() {
        val event = Event(senderId = A_SENDER_ID)
        val beaconLocationData = MessageBeaconLocationDataContent()

        val result = liveLocationAggregationProcessor.handleBeaconLocationData(
                realm = fakeRealm.instance,
                event = event,
                content = beaconLocationData,
                roomId = A_ROOM_ID,
                relatedEventId = AN_EVENT_ID,
                isLocalEcho = true
        )

        result shouldBeEqualTo false
    }

    @Test
    fun `given beacon location data when relatedEventId is null or empty then it is ignored`() {
        val event = Event(senderId = A_SENDER_ID)
        val beaconLocationData = MessageBeaconLocationDataContent()

        val resultNoRelatedEventId = liveLocationAggregationProcessor.handleBeaconLocationData(
                realm = fakeRealm.instance,
                event = event,
                content = beaconLocationData,
                roomId = A_ROOM_ID,
                relatedEventId = null,
                isLocalEcho = false
        )
        val resultEmptyRelatedEventId = liveLocationAggregationProcessor.handleBeaconLocationData(
                realm = fakeRealm.instance,
                event = event,
                content = beaconLocationData,
                roomId = A_ROOM_ID,
                relatedEventId = "",
                isLocalEcho = false
        )

        resultNoRelatedEventId shouldBeEqualTo false
        resultEmptyRelatedEventId shouldBeEqualTo false
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
                relatedEventId = AN_EVENT_ID,
                isLocalEcho = false
        )
        val resultEmptySenderId = liveLocationAggregationProcessor.handleBeaconLocationData(
                realm = fakeRealm.instance,
                event = eventEmptySenderId,
                content = beaconLocationData,
                roomId = "",
                relatedEventId = AN_EVENT_ID,
                isLocalEcho = false
        )

        resultNoSenderId shouldBeEqualTo false
        resultEmptySenderId shouldBeEqualTo false
    }

    private fun mockLiveLocationShareAggregatedSummaryEntityForEvent(): LiveLocationShareAggregatedSummaryEntity {
        val result = LiveLocationShareAggregatedSummaryEntity(
                eventId = AN_EVENT_ID,
                roomId = A_ROOM_ID
        )
        fakeQuery
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, AN_EVENT_ID)
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.ROOM_ID, A_ROOM_ID)
                .givenFindFirst(result)
        return result
    }

    private fun mockPreviousLiveLocationShareAggregatedSummaryEntities(): List<LiveLocationShareAggregatedSummaryEntity> {
        val results = listOf(
                LiveLocationShareAggregatedSummaryEntity(
                        eventId = "",
                        roomId = A_ROOM_ID,
                        userId = A_SENDER_ID,
                        isActive = true
                )
        )
        fakeQuery
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.ROOM_ID, A_ROOM_ID)
                .givenNotEqualTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, AN_EVENT_ID)
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.USER_ID, A_SENDER_ID)
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.IS_ACTIVE, true)
                .givenFindAll(results)
        return results
    }
}
