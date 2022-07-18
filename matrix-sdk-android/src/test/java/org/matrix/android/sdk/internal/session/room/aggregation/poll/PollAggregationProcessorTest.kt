/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.aggregation.poll

import io.mockk.every
import io.mockk.mockk
import io.realm.RealmList
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntity
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.AN_EVENT_ID
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.AN_INVALID_POLL_RESPONSE_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_BROKEN_POLL_REPLACE_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_POLL_END_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_POLL_REFERENCE_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_POLL_REPLACE_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_POLL_RESPONSE_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_POLL_RESPONSE_EVENT_WITH_A_WRONG_REFERENCE
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_POLL_START_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_ROOM_ID
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_TIMELINE_EVENT
import org.matrix.android.sdk.internal.session.room.aggregation.poll.PollEventsTestData.A_USER_ID_1
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindFirst

class PollAggregationProcessorTest {

    private val pollAggregationProcessor: PollAggregationProcessor = DefaultPollAggregationProcessor()
    private val realm = FakeRealm()
    private val session = mockk<Session>()

    @Before
    fun setup() {
        mockEventAnnotationsSummaryEntity()
        mockRoom(A_ROOM_ID, AN_EVENT_ID)
        every { session.myUserId } returns A_USER_ID_1
    }

    @Test
    fun `given a poll start event, when processing, then is ignored and returns false`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_POLL_START_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll start event with a reference, when processing, then is ignored and returns false`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_POLL_REFERENCE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll start event with a replace relation but without a target event id, when processing, then is ignored and returns false`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_BROKEN_POLL_REPLACE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll start event with a replace, when processing, then is processed and returns true`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_POLL_REPLACE_EVENT).shouldBeTrue()
    }

    @Test
    fun `given a poll response event with a broken reference, when processing, then is ignored and returns false`() {
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT_WITH_A_WRONG_REFERENCE).shouldBeFalse()
    }

    @Test
    fun `given a poll response event with a reference, when processing, then is processed and returns true`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity()
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT).shouldBeTrue()
    }

    @Test
    fun `given a poll response event after poll is closed, when processing, then is ignored and returns false`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity().apply {
            closedTime = (A_POLL_RESPONSE_EVENT.originServerTs ?: 0) - 1
        }
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll response event which is already processed, when processing, then is ignored and returns false`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity().apply {
            sourceEvents = RealmList(A_POLL_RESPONSE_EVENT.eventId)
        }
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll response event which is not one of the options, when processing, then is ignored and returns false`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity()
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, AN_INVALID_POLL_RESPONSE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll end event, when processing, then is processed and return true`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity()
        val powerLevelsHelper = mockRedactionPowerLevels(A_USER_ID_1, true)
        pollAggregationProcessor.handlePollEndEvent(session, powerLevelsHelper, realm.instance, A_POLL_END_EVENT).shouldBeTrue()
    }

    @Test
    fun `given a poll end event for my own poll without enough redaction power level, when processing, then is processed and returns true`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity()
        val powerLevelsHelper = mockRedactionPowerLevels(A_USER_ID_1, false)
        pollAggregationProcessor.handlePollEndEvent(session, powerLevelsHelper, realm.instance, A_POLL_END_EVENT).shouldBeTrue()
    }

    @Test
    fun `given a poll end event without enough redaction power level, when is processed, then is ignored and return false`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity()
        val powerLevelsHelper = mockRedactionPowerLevels("another-sender-id", false)
        val event = A_POLL_END_EVENT.copy(senderId = "another-sender-id")
        pollAggregationProcessor.handlePollEndEvent(session, powerLevelsHelper, realm.instance, event).shouldBeFalse()
    }

    private fun mockEventAnnotationsSummaryEntity() {
        realm.givenWhere<EventAnnotationsSummaryEntity>()
                .givenFindFirst(EventAnnotationsSummaryEntity())
                .givenEqualTo(EventAnnotationsSummaryEntityFields.ROOM_ID, A_POLL_REPLACE_EVENT.roomId!!)
                .givenEqualTo(EventAnnotationsSummaryEntityFields.EVENT_ID, A_POLL_REPLACE_EVENT.eventId!!)
    }

    private fun mockRoom(
            roomId: String,
            eventId: String
    ) {
        val room = mockk<Room>()
        every { session.getRoom(roomId) } returns room
        every { room.getTimelineEvent(eventId) } returns A_TIMELINE_EVENT
    }

    private fun mockRedactionPowerLevels(userId: String, isAbleToRedact: Boolean): PowerLevelsHelper {
        val powerLevelsHelper = mockk<PowerLevelsHelper>()
        every { powerLevelsHelper.isUserAbleToRedact(userId) } returns isAbleToRedact
        return powerLevelsHelper
    }
}
