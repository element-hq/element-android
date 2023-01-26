/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.event

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
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.test.fakes.FakeClock
import org.matrix.android.sdk.test.fakes.FakeEventDecryptor
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.givenFindAll
import org.matrix.android.sdk.test.fakes.givenIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultFilterAndStoreEventsTaskTest {

    private val fakeMonarchy = FakeMonarchy()
    private val fakeClock = FakeClock()
    private val fakeEventDecryptor = FakeEventDecryptor()

    private val defaultFilterAndStoreEventsTask = DefaultFilterAndStoreEventsTask(
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
    fun `given a room and list of events when execute then filter in using given predicate and store them in local if needed`() = runTest {
        // Given
        val aRoomId = "roomId"
        val anEventId1 = "eventId1"
        val anEventId2 = "eventId2"
        val anEventId3 = "eventId3"
        val anEventId4 = "eventId4"
        val event1 = givenAnEvent(eventId = anEventId1, isEncrypted = true, clearType = EventType.ENCRYPTED)
        val event2 = givenAnEvent(eventId = anEventId2, isEncrypted = true, clearType = EventType.MESSAGE)
        val event3 = givenAnEvent(eventId = anEventId3, isEncrypted = false, clearType = EventType.MESSAGE)
        val event4 = givenAnEvent(eventId = anEventId4, isEncrypted = false, clearType = EventType.MESSAGE)
        val events = listOf(event1, event2, event3, event4)
        val filterPredicate = { event: Event -> event == event2 }
        val params = givenTaskParams(roomId = aRoomId, events = events, predicate = filterPredicate)
        fakeEventDecryptor.givenDecryptEventAndSaveResultSuccess(event1)
        fakeEventDecryptor.givenDecryptEventAndSaveResultSuccess(event2)
        fakeClock.givenEpoch(123)
        givenExistingEventEntities(eventIdsToCheck = listOf(anEventId1, anEventId2), existingIds = listOf(anEventId1))
        val eventEntityToSave = EventEntity(eventId = anEventId2)
        every { event2.toEntity(any(), any(), any()) } returns eventEntityToSave
        every { eventEntityToSave.copyToRealmOrIgnore(any(), any()) } returns eventEntityToSave

        // When
        defaultFilterAndStoreEventsTask.execute(params)

        // Then
        fakeEventDecryptor.verifyDecryptEventAndSaveResult(event1, timeline = "")
        fakeEventDecryptor.verifyDecryptEventAndSaveResult(event2, timeline = "")
        // Check we save in DB the event2 which is a non stored poll response
        verify {
            event2.toEntity(aRoomId, SendState.SYNCED, any())
            eventEntityToSave.copyToRealmOrIgnore(fakeMonarchy.fakeRealm.instance, EventInsertType.PAGINATION)
        }
    }

    private fun givenTaskParams(roomId: String, events: List<Event>, predicate: (Event) -> Boolean) = FilterAndStoreEventsTask.Params(
            roomId = roomId,
            events = events,
            filterPredicate = predicate,
    )

    private fun givenAnEvent(
            eventId: String,
            isEncrypted: Boolean,
            clearType: String,
    ): Event {
        val event = mockk<Event>(relaxed = true)
        every { event.eventId } returns eventId
        every { event.isEncrypted() } returns isEncrypted
        every { event.getClearType() } returns clearType
        return event
    }

    private fun givenExistingEventEntities(eventIdsToCheck: List<String>, existingIds: List<String>) {
        val eventEntities = existingIds.map { EventEntity(eventId = it) }
        fakeMonarchy.givenWhere<EventEntity>()
                .givenIn(EventEntityFields.EVENT_ID, eventIdsToCheck)
                .givenFindAll(eventEntities)
    }
}
