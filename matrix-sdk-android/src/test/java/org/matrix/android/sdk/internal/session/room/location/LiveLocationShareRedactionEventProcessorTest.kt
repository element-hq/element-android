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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntityFields
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.givenDelete
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindFirst

private const val AN_EVENT_ID = "event-id"
private const val A_REDACTED_EVENT_ID = "redacted-event-id"

@ExperimentalCoroutinesApi
class LiveLocationShareRedactionEventProcessorTest {

    private val liveLocationShareRedactionEventProcessor = LiveLocationShareRedactionEventProcessor()
    private val fakeRealm = FakeRealm()

    @Test
    fun `given an event when checking if it should be processed then only event of type REDACTED is processed`() {
        val eventId = AN_EVENT_ID
        val eventType = EventType.REDACTION
        val insertType = EventInsertType.INCREMENTAL_SYNC

        val result = liveLocationShareRedactionEventProcessor.shouldProcess(
                eventId = eventId,
                eventType = eventType,
                insertType = insertType
        )

        result shouldBe true
    }

    @Test
    fun `given an event when checking if it should be processed then local echo is not processed`() {
        val eventId = AN_EVENT_ID
        val eventType = EventType.REDACTION
        val insertType = EventInsertType.LOCAL_ECHO

        val result = liveLocationShareRedactionEventProcessor.shouldProcess(
                eventId = eventId,
                eventType = eventType,
                insertType = insertType
        )

        result shouldBe false
    }

    @Test
    fun `given a redacted live location share event when processing it then related summaries are deleted from database`() = runTest {
        val event = Event(eventId = AN_EVENT_ID, redacts = A_REDACTED_EVENT_ID)
        val redactedEventEntity = EventEntity(eventId = A_REDACTED_EVENT_ID, type = EventType.STATE_ROOM_BEACON_INFO.unstable)
        fakeRealm.givenWhere<EventEntity>()
                .givenEqualTo(EventEntityFields.EVENT_ID, A_REDACTED_EVENT_ID)
                .givenFindFirst(redactedEventEntity)
        val liveSummary = mockk<LiveLocationShareAggregatedSummaryEntity>()
        every { liveSummary.eventId } returns A_REDACTED_EVENT_ID
        liveSummary.givenDelete()
        fakeRealm.givenWhere<LiveLocationShareAggregatedSummaryEntity>()
                .givenEqualTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, A_REDACTED_EVENT_ID)
                .givenFindFirst(liveSummary)
        val annotationsSummary = mockk<EventAnnotationsSummaryEntity>()
        every { annotationsSummary.eventId } returns A_REDACTED_EVENT_ID
        annotationsSummary.givenDelete()
        fakeRealm.givenWhere<EventAnnotationsSummaryEntity>()
                .givenEqualTo(EventAnnotationsSummaryEntityFields.EVENT_ID, A_REDACTED_EVENT_ID)
                .givenFindFirst(annotationsSummary)

        liveLocationShareRedactionEventProcessor.process(fakeRealm.instance, event = event)

        verify {
            liveSummary.deleteFromRealm()
            annotationsSummary.deleteFromRealm()
        }
    }
}
