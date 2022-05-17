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
import io.realm.RealmModel
import io.realm.RealmQuery
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollResponseContent
import org.matrix.android.sdk.api.session.room.model.message.PollAnswer
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.model.message.PollResponse
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.PollResponseAggregatedSummaryEntity
import org.matrix.android.sdk.test.fakes.FakeRealm

private const val A_USER_ID_1 = "@user_1:matrix.org"
private const val A_USER_ID_2 = "@user_2:matrix.org"
private const val A_ROOM_ID = "!sUeOGZKsBValPTUMax:matrix.org"
private const val AN_EVENT_ID = "\$vApgexcL8Vfh-WxYKsFKCDooo67ttbjm3TiVKXaWijU"

private val A_POLL_CONTENT = MessagePollContent(
        unstablePollCreationInfo = PollCreationInfo(
                question = PollQuestion(
                        unstableQuestion = "What is your favourite coffee?"
                ),
                maxSelections = 1,
                answers = listOf(
                        PollAnswer(
                                id = "5ef5f7b0-c9a1-49cf-a0b3-374729a43e76",
                                unstableAnswer = "Double Espresso"
                        ),
                        PollAnswer(
                                id = "ec1a4db0-46d8-4d7a-9bb6-d80724715938",
                                unstableAnswer = "Macchiato"
                        ),
                        PollAnswer(
                                id = "3677ca8e-061b-40ab-bffe-b22e4e88fcad",
                                unstableAnswer = "Iced Coffee"
                        )
                )
        )
)

private val A_POLL_RESPONSE_CONTENT = MessagePollResponseContent(
        unstableResponse = PollResponse(
                answers = listOf("5ef5f7b0-c9a1-49cf-a0b3-374729a43e76")
        ),
        relatesTo = RelationDefaultContent(
                type = RelationType.REFERENCE,
                eventId = AN_EVENT_ID
        )
)

private val AN_INVALID_POLL_RESPONSE_CONTENT = MessagePollResponseContent(
        unstableResponse = PollResponse(
                answers = listOf("fake-option-id")
        ),
        relatesTo = RelationDefaultContent(
                type = RelationType.REFERENCE,
                eventId = AN_EVENT_ID
        )
)

private val A_POLL_START_EVENT = Event(
        type = EventType.POLL_START.first(),
        eventId = AN_EVENT_ID,
        originServerTs = 1652435922563,
        senderId = A_USER_ID_1,
        roomId = A_ROOM_ID,
        content = A_POLL_CONTENT.toContent()
)

private val A_POLL_RESPONSE_EVENT = Event(
        type = EventType.POLL_RESPONSE.first(),
        eventId = AN_EVENT_ID,
        originServerTs = 1652435922563,
        senderId = A_USER_ID_1,
        roomId = A_ROOM_ID,
        content = A_POLL_RESPONSE_CONTENT.toContent()
)

private val A_TIMELINE_EVENT = TimelineEvent(
        root = A_POLL_START_EVENT,
        localId = 1234,
        eventId = AN_EVENT_ID,
        displayIndex = 0,
        senderInfo = SenderInfo(A_USER_ID_1, "A_USER_ID_1", true, null)
)

private val A_POLL_RESPONSE_EVENT_WITHOUT_REFERENCE = A_POLL_RESPONSE_EVENT.copy(
        content = A_POLL_RESPONSE_CONTENT
                .copy(
                        relatesTo = RelationDefaultContent(
                                type = RelationType.REFERENCE,
                                eventId = null
                        )
                )
                .toContent()
)

private val A_POLL_RESPONSE_EVENT_WITH_A_WRONG_REFERENCE = A_POLL_RESPONSE_EVENT.copy(
        content = A_POLL_RESPONSE_CONTENT
                .copy(
                        relatesTo = RelationDefaultContent(
                                type = RelationType.REPLACE,
                                eventId = null
                        )
                )
                .toContent()
)

private val A_POLL_REPLACE_EVENT = A_POLL_START_EVENT.copy(
        content = A_POLL_CONTENT
                .copy(
                        relatesTo = RelationDefaultContent(
                                type = RelationType.REPLACE,
                                eventId = AN_EVENT_ID
                        )
                )
                .toContent()
)

private val A_BROKEN_POLL_REPLACE_EVENT = A_POLL_START_EVENT.copy(
        content = A_POLL_CONTENT
                .copy(
                        relatesTo = RelationDefaultContent(
                                type = RelationType.REPLACE,
                                eventId = null
                        )
                )
                .toContent()
)

private val A_POLL_REFERENCE_EVENT = A_POLL_START_EVENT.copy(
        content = A_POLL_CONTENT
                .copy(
                        relatesTo = RelationDefaultContent(
                                type = RelationType.REFERENCE,
                                eventId = AN_EVENT_ID
                        )
                )
                .toContent()
)

private val AN_INVALID_POLL_RESPONSE_EVENT = A_POLL_RESPONSE_EVENT.copy(
        content = AN_INVALID_POLL_RESPONSE_CONTENT.toContent()
)

private val AN_EVENT_ANNOTATIONS_SUMMARY_ENTITY = EventAnnotationsSummaryEntity(
        roomId = A_ROOM_ID,
        eventId = AN_EVENT_ID,
        pollResponseSummary = PollResponseAggregatedSummaryEntity()
)

class PollAggregationProcessorTest {

    private val pollAggregationProcessor: PollAggregationProcessor = DefaultPollAggregationProcessor()
    private val realm = FakeRealm()
    private val session = mockk<Session>()

    @Before
    fun setup() {
        mockEventAnnotationsSummaryEntity()
        mockRoom(A_ROOM_ID, AN_EVENT_ID)
    }

    @Test
    fun `given a poll start event which is not a replace is not processed by poll aggregator`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_POLL_START_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll start event with a reference is not processed by poll aggregator`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_POLL_REFERENCE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll start event with a replace but without target event id is not processed by poll aggregator`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_BROKEN_POLL_REPLACE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll start event with a replace is processed by poll aggregator`() {
        pollAggregationProcessor.handlePollStartEvent(realm.instance, A_POLL_REPLACE_EVENT).shouldBeTrue()
    }

    @Test
    fun `given a poll response event with a broken reference is not processed by poll aggregator`() {
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT_WITH_A_WRONG_REFERENCE).shouldBeFalse()
    }

    @Test
    fun `given a poll response event with a reference is processed by poll aggregator`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity()
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT).shouldBeTrue()
    }

    @Test
    fun `given a poll response event after poll is closed is not processed by poll aggregator`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity().apply {
            closedTime = (A_POLL_RESPONSE_EVENT.originServerTs ?: 0) - 1
        }
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll response event which is already processed is not processed by poll aggregator`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity().apply {
            sourceEvents = RealmList(A_POLL_RESPONSE_EVENT.eventId)
        }
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, A_POLL_RESPONSE_EVENT).shouldBeFalse()
    }

    @Test
    fun `given a poll response event which is not one of the options is not processed by poll aggregator`() {
        every { realm.instance.createObject(PollResponseAggregatedSummaryEntity::class.java) } returns PollResponseAggregatedSummaryEntity()
        pollAggregationProcessor.handlePollResponseEvent(session, realm.instance, AN_INVALID_POLL_RESPONSE_EVENT).shouldBeFalse()
    }

    @Test
    fun handlePollEndEvent() {
    }

    private inline fun <reified T : RealmModel> RealmQuery<T>.givenEqualTo(fieldName: String, value: String, result: RealmQuery<T>) {
        every { equalTo(fieldName, value) } returns result
    }

    private fun mockEventAnnotationsSummaryEntity() {
        val queryResult = realm.givenWhereReturns(result = EventAnnotationsSummaryEntity())
        queryResult.givenEqualTo(EventAnnotationsSummaryEntityFields.ROOM_ID, A_POLL_REPLACE_EVENT.roomId!!, queryResult)
        queryResult.givenEqualTo(EventAnnotationsSummaryEntityFields.EVENT_ID, A_POLL_REPLACE_EVENT.eventId!!, queryResult)
    }

    private fun mockRoom(
            roomId: String,
            eventId: String
    ) {
        val room = mockk<Room>()
        every { session.getRoom(roomId) } returns room
        every { room.getTimelineEvent(eventId) } returns A_TIMELINE_EVENT
    }
}
