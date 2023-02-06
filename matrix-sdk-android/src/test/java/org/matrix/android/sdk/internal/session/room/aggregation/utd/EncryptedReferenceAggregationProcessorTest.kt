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

package org.matrix.android.sdk.internal.session.room.aggregation.utd

import io.mockk.every
import io.mockk.mockk
import io.realm.RealmList
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntityFields
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.givenContainsValue
import org.matrix.android.sdk.test.fakes.givenFindFirst

internal class EncryptedReferenceAggregationProcessorTest {

    private val fakeRealm = FakeRealm()

    private val encryptedReferenceAggregationProcessor = EncryptedReferenceAggregationProcessor()

    @Test
    fun `given local echo when process then result is false`() {
        // Given
        val anEvent = mockk<Event>()
        val isLocalEcho = true
        val relatedEventId = "event-id"

        // When
        val result = encryptedReferenceAggregationProcessor.handle(
                realm = fakeRealm.instance,
                event = anEvent,
                isLocalEcho = isLocalEcho,
                relatedEventId = relatedEventId,
        )

        // Then
        result.shouldBeFalse()
    }

    @Test
    fun `given invalid event id when process then result is false`() {
        // Given
        val anEvent = mockk<Event>()
        val isLocalEcho = false

        // When
        val result1 = encryptedReferenceAggregationProcessor.handle(
                realm = fakeRealm.instance,
                event = anEvent,
                isLocalEcho = isLocalEcho,
                relatedEventId = null,
        )
        val result2 = encryptedReferenceAggregationProcessor.handle(
                realm = fakeRealm.instance,
                event = anEvent,
                isLocalEcho = isLocalEcho,
                relatedEventId = "",
        )

        // Then
        result1.shouldBeFalse()
        result2.shouldBeFalse()
    }

    @Test
    fun `given related event id of an existing poll when process then result is true and event id is stored in poll summary`() {
        // Given
        val anEventId = "event-id"
        val anEvent = givenAnEvent(anEventId)
        val isLocalEcho = false
        val relatedEventId = "related-event-id"
        val pollResponseAggregatedSummaryEntity = PollResponseAggregatedSummaryEntity(
                encryptedRelatedEventIds = RealmList(),
        )
        fakeRealm.givenWhere<PollResponseAggregatedSummaryEntity>()
                .givenContainsValue(PollResponseAggregatedSummaryEntityFields.SOURCE_EVENTS.`$`, relatedEventId)
                .givenFindFirst(pollResponseAggregatedSummaryEntity)

        // When
        val result = encryptedReferenceAggregationProcessor.handle(
                realm = fakeRealm.instance,
                event = anEvent,
                isLocalEcho = isLocalEcho,
                relatedEventId = relatedEventId,
        )

        // Then
        result.shouldBeTrue()
        pollResponseAggregatedSummaryEntity.encryptedRelatedEventIds.shouldContain(anEventId)
    }

    @Test
    fun `given related event id but no existing related poll when process then result is true and event id is not stored`() {
        // Given
        val anEventId = "event-id"
        val anEvent = givenAnEvent(anEventId)
        val isLocalEcho = false
        val relatedEventId = "related-event-id"
        fakeRealm.givenWhere<PollResponseAggregatedSummaryEntity>()
                .givenContainsValue(PollResponseAggregatedSummaryEntityFields.SOURCE_EVENTS.`$`, relatedEventId)
                .givenFindFirst(null)

        // When
        val result = encryptedReferenceAggregationProcessor.handle(
                realm = fakeRealm.instance,
                event = anEvent,
                isLocalEcho = isLocalEcho,
                relatedEventId = relatedEventId,
        )

        // Then
        result.shouldBeTrue()
    }

    private fun givenAnEvent(eventId: String): Event {
        return mockk<Event>().also {
            every { it.eventId } returns eventId
        }
    }
}
