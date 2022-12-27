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
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.internal.session.room.aggregation.utd.FakeEncryptedReferenceAggregationProcessor

class EncryptedEventRelationsAggregationProcessorTest {

    private val fakeEncryptedReferenceAggregationProcessor = FakeEncryptedReferenceAggregationProcessor()
    private val fakeRealm = FakeRealm()

    private val encryptedEventRelationsAggregationProcessor = EncryptedEventRelationsAggregationProcessor(
            encryptedReferenceAggregationProcessor = fakeEncryptedReferenceAggregationProcessor.instance,
    )

    @Test
    fun `given no room Id when process then result is false`() {
        // Given
        val anEvent = givenAnEvent(
                eventId = "event-id",
                roomId = null,
                eventType = EventType.ENCRYPTED,
        )

        // When
        val result = encryptedEventRelationsAggregationProcessor.process(
                realm = fakeRealm.instance,
                event = anEvent,
        )

        // Then
        result.shouldBeFalse()
    }

    @Test
    fun `given an encrypted reference event when process then reference is processed`() {
        // Given
        val anEvent = givenAnEvent(
                eventId = "event-id",
                roomId = "room-id",
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

        // When
        val result = encryptedEventRelationsAggregationProcessor.process(
                realm = fakeRealm.instance,
                event = anEvent,
        )

        // Then
        result shouldBeEqualTo resultOfReferenceProcess
        fakeEncryptedReferenceAggregationProcessor.verifyHandle(
                realm = fakeRealm.instance,
                event = anEvent,
                isLocalEcho = false,
                relatedEventId = relatedEventId,
        )
    }

    @Test
    fun `given an encrypted replace event when process then result is false`() {
        // Given
        val anEvent = givenAnEvent(
                eventId = "event-id",
                roomId = "room-id",
                eventType = EventType.ENCRYPTED,
        )
        val relatedEventId = "related-event-id"
        val encryptedEventContent = givenEncryptedEventContent(
                relationType = RelationType.REPLACE,
                relatedEventId = relatedEventId,
        )
        every { anEvent.content } returns encryptedEventContent.toContent()

        // When
        val result = encryptedEventRelationsAggregationProcessor.process(
                realm = fakeRealm.instance,
                event = anEvent,
        )

        // Then
        result.shouldBeFalse()
    }

    @Test
    fun `given an encrypted response event when process then result is false`() {
        // Given
        val anEvent = givenAnEvent(
                eventId = "event-id",
                roomId = "room-id",
                eventType = EventType.ENCRYPTED,
        )
        val relatedEventId = "related-event-id"
        val encryptedEventContent = givenEncryptedEventContent(
                relationType = RelationType.RESPONSE,
                relatedEventId = relatedEventId,
        )
        every { anEvent.content } returns encryptedEventContent.toContent()

        // When
        val result = encryptedEventRelationsAggregationProcessor.process(
                realm = fakeRealm.instance,
                event = anEvent,
        )

        // Then
        result.shouldBeFalse()
    }

    @Test
    fun `given an encrypted annotation event when process then result is false`() {
        // Given
        val anEvent = givenAnEvent(
                eventId = "event-id",
                roomId = "room-id",
                eventType = EventType.ENCRYPTED,
        )
        val relatedEventId = "related-event-id"
        val encryptedEventContent = givenEncryptedEventContent(
                relationType = RelationType.ANNOTATION,
                relatedEventId = relatedEventId,
        )
        every { anEvent.content } returns encryptedEventContent.toContent()

        // When
        val result = encryptedEventRelationsAggregationProcessor.process(
                realm = fakeRealm.instance,
                event = anEvent,
        )

        // Then
        result.shouldBeFalse()
    }

    @Test
    fun `given a non encrypted event when process then result is false`() {
        // Given
        val anEvent = givenAnEvent(
                eventId = "event-id",
                roomId = "room-id",
                eventType = EventType.MESSAGE,
        )

        // When
        val result = encryptedEventRelationsAggregationProcessor.process(
                realm = fakeRealm.instance,
                event = anEvent,
        )

        // Then
        result.shouldBeFalse()
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
}
