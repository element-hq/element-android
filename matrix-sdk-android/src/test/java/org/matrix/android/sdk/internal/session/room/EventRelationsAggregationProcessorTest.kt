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

package org.matrix.android.sdk.internal.session.room

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntityFields
import org.matrix.android.sdk.test.fakes.FakeClock
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.FakeStateEventDataSource
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindFirst
import org.matrix.android.sdk.test.fakes.internal.FakeEventEditValidator
import org.matrix.android.sdk.test.fakes.internal.FakeLiveLocationAggregationProcessor
import org.matrix.android.sdk.test.fakes.internal.FakePollAggregationProcessor
import org.matrix.android.sdk.test.fakes.internal.FakeSessionManager
import org.matrix.android.sdk.test.fakes.internal.session.room.aggregation.utd.FakeEncryptedReferenceAggregationProcessor

private const val A_ROOM_ID = "room-id"
private const val AN_EVENT_ID = "event-id"

internal class EventRelationsAggregationProcessorTest {

    private val fakeStateEventDataSource = FakeStateEventDataSource()
    private val fakeSessionManager = FakeSessionManager()
    private val fakeLiveLocationAggregationProcessor = FakeLiveLocationAggregationProcessor()
    private val fakePollAggregationProcessor = FakePollAggregationProcessor()
    private val fakeEncryptedReferenceAggregationProcessor = FakeEncryptedReferenceAggregationProcessor()
    private val fakeEventEditValidator = FakeEventEditValidator()
    private val fakeClock = FakeClock()
    private val fakeRealm = FakeRealm()

    private val encryptedEventRelationsAggregationProcessor = EventRelationsAggregationProcessor(
            userId = "userId",
            stateEventDataSource = fakeStateEventDataSource.instance,
            sessionId = "sessionId",
            sessionManager = fakeSessionManager.instance,
            liveLocationAggregationProcessor = fakeLiveLocationAggregationProcessor.instance,
            pollAggregationProcessor = fakePollAggregationProcessor.instance,
            encryptedReferenceAggregationProcessor = fakeEncryptedReferenceAggregationProcessor.instance,
            editValidator = fakeEventEditValidator.instance,
            clock = fakeClock,
    )

    @Test
    fun `given an encrypted reference event when process then reference is processed`() {
        // Given
        val anEvent = givenAnEvent(
                eventId = AN_EVENT_ID,
                roomId = A_ROOM_ID,
                eventType = EventType.ENCRYPTED,
        )
        val relatedEventId = "related-event-id"
        val encryptedEventContent = givenEncryptedEventContent(
                relationType = RelationType.REFERENCE,
                relatedEventId = relatedEventId,
        )
        every { anEvent.content } returns encryptedEventContent.toContent()
        val resultOfReferenceProcess = false
        fakeEncryptedReferenceAggregationProcessor.givenHandleReturns(resultOfReferenceProcess)
        givenEventAnnotationsSummary(roomId = A_ROOM_ID, eventId = AN_EVENT_ID, annotationsSummary = null)

        // When
        encryptedEventRelationsAggregationProcessor.process(
                realm = fakeRealm.instance,
                event = anEvent,
        )

        // Then
        fakeEncryptedReferenceAggregationProcessor.verifyHandle(
                realm = fakeRealm.instance,
                event = anEvent,
                isLocalEcho = false,
                relatedEventId = relatedEventId,
        )
    }

    private fun givenAnEvent(
            eventId: String,
            roomId: String?,
            eventType: String,
    ): Event {
        return mockk<Event>().also {
            every { it.eventId } returns eventId
            every { it.roomId } returns roomId
            every { it.getClearType() } returns eventType
        }
    }

    private fun givenEncryptedEventContent(relationType: String, relatedEventId: String): EncryptedEventContent {
        val relationContent = RelationDefaultContent(
                eventId = relatedEventId,
                type = relationType,
        )
        return EncryptedEventContent(
                relatesTo = relationContent,
        )
    }

    private fun givenEventAnnotationsSummary(
            roomId: String,
            eventId: String,
            annotationsSummary: EventAnnotationsSummaryEntity?
    ) {
        fakeRealm.givenWhere<EventAnnotationsSummaryEntity>()
                .givenEqualTo(EventAnnotationsSummaryEntityFields.ROOM_ID, roomId)
                .givenEqualTo(EventAnnotationsSummaryEntityFields.EVENT_ID, eventId)
                .givenFindFirst(annotationsSummary)
    }
}
