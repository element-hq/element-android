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

package org.matrix.android.sdk.internal.session.room.relation.poll

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.isPollResponse
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.session.room.relation.RelationsResponse
import org.matrix.android.sdk.test.fakes.FakeClock
import org.matrix.android.sdk.test.fakes.FakeEventDecryptor
import org.matrix.android.sdk.test.fakes.FakeGlobalErrorReceiver
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.FakeRoomApi
import org.matrix.android.sdk.test.fakes.givenFindAll
import org.matrix.android.sdk.test.fakes.givenIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultFetchPollResponseEventsTaskTest {

    private val fakeRoomAPI = FakeRoomApi()
    private val fakeGlobalErrorReceiver = FakeGlobalErrorReceiver()
    private val fakeMonarchy = FakeMonarchy()
    private val fakeClock = FakeClock()
    private val fakeEventDecryptor = FakeEventDecryptor()

    private val defaultFetchPollResponseEventsTask = DefaultFetchPollResponseEventsTask(
            roomAPI = fakeRoomAPI.instance,
            globalErrorReceiver = fakeGlobalErrorReceiver,
            monarchy = fakeMonarchy.instance,
            clock = fakeClock,
            eventDecryptor = fakeEventDecryptor.instance,
    )

    @Before
    fun setup() {
        mockkStatic("org.matrix.android.sdk.api.session.events.model.EventKt")
        mockkStatic("org.matrix.android.sdk.internal.database.mapper.EventMapperKt")
        mockkStatic("org.matrix.android.sdk.internal.database.query.EventEntityQueriesKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a room and a poll when execute then fetch related events and store them in local if needed`() = runTest {
        // Given
        val aRoomId = "roomId"
        val aPollEventId = "eventId"
        val params = givenTaskParams(roomId = aRoomId, eventId = aPollEventId)
        val aNextBatchToken = "nextBatch"
        val anEventId1 = "eventId1"
        val anEventId2 = "eventId2"
        val anEventId3 = "eventId3"
        val anEventId4 = "eventId4"
        val event1 = givenAnEvent(eventId = anEventId1, isPollResponse = true, isEncrypted = true)
        val event2 = givenAnEvent(eventId = anEventId2, isPollResponse = true, isEncrypted = true)
        val event3 = givenAnEvent(eventId = anEventId3, isPollResponse = false, isEncrypted = false)
        val event4 = givenAnEvent(eventId = anEventId4, isPollResponse = false, isEncrypted = false)
        val firstEvents = listOf(event1, event2)
        val secondEvents = listOf(event3, event4)
        val firstResponse = givenARelationsResponse(events = firstEvents, nextBatch = aNextBatchToken)
        fakeRoomAPI.givenGetRelationsReturns(from = null, relationsResponse = firstResponse)
        val secondResponse = givenARelationsResponse(events = secondEvents, nextBatch = null)
        fakeRoomAPI.givenGetRelationsReturns(from = aNextBatchToken, relationsResponse = secondResponse)
        fakeEventDecryptor.givenDecryptEventAndSaveResultSuccess(event1)
        fakeEventDecryptor.givenDecryptEventAndSaveResultSuccess(event2)
        fakeClock.givenEpoch(123)
        givenExistingEventEntities(eventIdsToCheck = listOf(anEventId1, anEventId2), existingIds = listOf(anEventId1))
        val eventEntityToSave = EventEntity(eventId = anEventId2)
        every { event2.toEntity(any(), any(), any()) } returns eventEntityToSave
        every { eventEntityToSave.copyToRealmOrIgnore(any(), any()) } returns eventEntityToSave

        // When
        defaultFetchPollResponseEventsTask.execute(params)

        // Then
        fakeRoomAPI.verifyGetRelations(
                roomId = params.roomId,
                eventId = params.startPollEventId,
                relationType = RelationType.REFERENCE,
                from = null,
                limit = FETCH_RELATED_EVENTS_LIMIT
        )
        fakeRoomAPI.verifyGetRelations(
                roomId = params.roomId,
                eventId = params.startPollEventId,
                relationType = RelationType.REFERENCE,
                from = aNextBatchToken,
                limit = FETCH_RELATED_EVENTS_LIMIT
        )
        fakeEventDecryptor.verifyDecryptEventAndSaveResult(event1, timeline = "")
        fakeEventDecryptor.verifyDecryptEventAndSaveResult(event2, timeline = "")
        // Check we save in DB the event2 which is a non stored poll response
        verify {
            event2.toEntity(aRoomId, SendState.SYNCED, any())
            eventEntityToSave.copyToRealmOrIgnore(fakeMonarchy.fakeRealm.instance, EventInsertType.PAGINATION)
        }
    }

    private fun givenTaskParams(roomId: String, eventId: String) = FetchPollResponseEventsTask.Params(
            roomId = roomId,
            startPollEventId = eventId,
    )

    private fun givenARelationsResponse(events: List<Event>, nextBatch: String?): RelationsResponse {
        return RelationsResponse(
                chunks = events,
                nextBatch = nextBatch,
                prevBatch = null,
        )
    }

    private fun givenAnEvent(
            eventId: String,
            isPollResponse: Boolean,
            isEncrypted: Boolean,
    ): Event {
        val event = mockk<Event>(relaxed = true)
        every { event.eventId } returns eventId
        every { event.isPollResponse() } returns isPollResponse
        every { event.isEncrypted() } returns isEncrypted
        return event
    }

    private fun givenExistingEventEntities(eventIdsToCheck: List<String>, existingIds: List<String>) {
        val eventEntities = existingIds.map { EventEntity(eventId = it) }
        fakeMonarchy.givenWhere<EventEntity>()
                .givenIn(EventEntityFields.EVENT_ID, eventIdsToCheck)
                .givenFindAll(eventEntities)
    }
}
