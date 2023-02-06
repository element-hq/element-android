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

package org.matrix.android.sdk.internal.session.room.location

import io.mockk.unmockkAll
import io.realm.RealmList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntityFields
import org.matrix.android.sdk.test.fakes.FakeEventSenderProcessor
import org.matrix.android.sdk.test.fakes.FakeLocalEchoEventFactory
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.FakeRealmConfiguration
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindFirst

private const val A_ROOM_ID = "room-id"
private const val AN_EVENT_ID = "event-id"
private const val AN_EVENT_ID_1 = "event-id-1"
private const val AN_EVENT_ID_2 = "event-id-2"
private const val AN_EVENT_ID_3 = "event-id-3"
private const val A_REASON = "reason"

@ExperimentalCoroutinesApi
class DefaultRedactLiveLocationShareTaskTest {

    private val fakeRealmConfiguration = FakeRealmConfiguration()
    private val fakeLocalEchoEventFactory = FakeLocalEchoEventFactory()
    private val fakeEventSenderProcessor = FakeEventSenderProcessor()
    private val fakeRealm = FakeRealm()

    private val defaultRedactLiveLocationShareTask = DefaultRedactLiveLocationShareTask(
            realmConfiguration = fakeRealmConfiguration.instance,
            localEchoEventFactory = fakeLocalEchoEventFactory.instance,
            eventSenderProcessor = fakeEventSenderProcessor
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given parameters when redacting then post redact events and related and creates redact local echos`() = runTest {
        val params = createParams()
        val relatedEventIds = listOf(AN_EVENT_ID_1, AN_EVENT_ID_2, AN_EVENT_ID_3)
        val aggregatedSummaryEntity = createSummary(relatedEventIds)
        givenSummaryForId(AN_EVENT_ID, aggregatedSummaryEntity)
        fakeRealmConfiguration.givenAwaitTransaction<List<String>>(fakeRealm.instance)
        val redactEvents = givenCreateRedactEventWithLocalEcho(relatedEventIds + AN_EVENT_ID)
        givenPostRedaction(redactEvents)

        defaultRedactLiveLocationShareTask.execute(params)

        verifyCreateRedactEventForEventIds(relatedEventIds + AN_EVENT_ID)
        verifyCreateLocalEchoForEvents(redactEvents)
    }

    private fun createParams() = RedactLiveLocationShareTask.Params(
            roomId = A_ROOM_ID,
            beaconInfoEventId = AN_EVENT_ID,
            reason = A_REASON
    )

    private fun createSummary(relatedEventIds: List<String>): LiveLocationShareAggregatedSummaryEntity {
        return LiveLocationShareAggregatedSummaryEntity(
                eventId = AN_EVENT_ID,
                relatedEventIds = RealmList(*relatedEventIds.toTypedArray()),
        )
    }

    private fun givenSummaryForId(eventId: String, aggregatedSummaryEntity: LiveLocationShareAggregatedSummaryEntity) {
        fakeRealm.givenWhere<LiveLocationShareAggregatedSummaryEntity>()
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, eventId)
                .givenFindFirst(aggregatedSummaryEntity)
    }

    private fun givenCreateRedactEventWithLocalEcho(eventIds: List<String>): List<Event> {
        return eventIds.map { eventId ->
            fakeLocalEchoEventFactory.givenCreateRedactEvent(
                    eventId = eventId,
                    withLocalEcho = true
            )
        }
    }

    private fun givenPostRedaction(redactEvents: List<Event>) {
        redactEvents.forEach {
            fakeEventSenderProcessor.givenPostRedaction(event = it, reason = A_REASON)
        }
    }

    private fun verifyCreateRedactEventForEventIds(eventIds: List<String>) {
        eventIds.forEach { eventId ->
            fakeLocalEchoEventFactory.verifyCreateRedactEvent(
                    roomId = A_ROOM_ID,
                    eventId = eventId,
                    reason = A_REASON
            )
        }
    }

    private fun verifyCreateLocalEchoForEvents(events: List<Event>) {
        events.forEach { redactionEvent ->
            fakeLocalEchoEventFactory.verifyCreateLocalEcho(redactionEvent)
        }
    }
}
