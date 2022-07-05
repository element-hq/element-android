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
    fun `given parameters when calling the task then it is correctly executed`() = runTest {
        val params = RedactLiveLocationShareTask.Params(
                roomId = A_ROOM_ID,
                beaconInfoEventId = AN_EVENT_ID,
                reason = A_REASON
        )
        fakeRealmConfiguration.givenAwaitTransaction<List<String>>(fakeRealm.instance)
        val relatedEventIds = listOf(AN_EVENT_ID_1, AN_EVENT_ID_2, AN_EVENT_ID_3)
        val aggregatedSummaryEntity = LiveLocationShareAggregatedSummaryEntity(
                eventId = AN_EVENT_ID,
                relatedEventIds = RealmList(*relatedEventIds.toTypedArray()),
        )
        fakeRealm.givenWhere<LiveLocationShareAggregatedSummaryEntity>()
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, AN_EVENT_ID)
                .givenFindFirst(aggregatedSummaryEntity)
        val redactedEvent = fakeLocalEchoEventFactory.givenCreateRedactEvent(
                eventId = AN_EVENT_ID,
                withLocalEcho = true
        )
        fakeEventSenderProcessor.givenPostRedaction(event = redactedEvent, reason = A_REASON)
        val redactedEvent1 = fakeLocalEchoEventFactory.givenCreateRedactEvent(
                eventId = AN_EVENT_ID_1,
                withLocalEcho = true
        )
        fakeEventSenderProcessor.givenPostRedaction(event = redactedEvent1, reason = A_REASON)
        val redactedEvent2 = fakeLocalEchoEventFactory.givenCreateRedactEvent(
                eventId = AN_EVENT_ID_2,
                withLocalEcho = true
        )
        fakeEventSenderProcessor.givenPostRedaction(event = redactedEvent2, reason = A_REASON)
        val redactedEvent3 = fakeLocalEchoEventFactory.givenCreateRedactEvent(
                eventId = AN_EVENT_ID_3,
                withLocalEcho = true
        )
        fakeEventSenderProcessor.givenPostRedaction(event = redactedEvent3, reason = A_REASON)

        defaultRedactLiveLocationShareTask.execute(params)

        fakeLocalEchoEventFactory.verifyCreateRedactEvent(
                roomId = A_ROOM_ID,
                eventId = AN_EVENT_ID,
                reason = A_REASON
        )
        fakeLocalEchoEventFactory.verifyCreateLocalEcho(redactedEvent)
        relatedEventIds.forEach { eventId ->
            fakeLocalEchoEventFactory.verifyCreateRedactEvent(
                    roomId = A_ROOM_ID,
                    eventId = eventId,
                    reason = A_REASON
            )
        }
        fakeLocalEchoEventFactory.verifyCreateLocalEcho(redactedEvent1)
        fakeLocalEchoEventFactory.verifyCreateLocalEcho(redactedEvent2)
        fakeLocalEchoEventFactory.verifyCreateLocalEcho(redactedEvent3)
    }
}
