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

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessageEndPollContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollResponseContent
import org.matrix.android.sdk.api.session.room.model.message.PollAnswer
import org.matrix.android.sdk.api.session.room.model.message.PollCreationInfo
import org.matrix.android.sdk.api.session.room.model.message.PollQuestion
import org.matrix.android.sdk.api.session.room.model.message.PollResponse
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

object PollEventsTestData {
    internal const val A_USER_ID_1 = "@user_1:matrix.org"
    internal const val A_ROOM_ID = "!sUeOGZKsBValPTUMax:matrix.org"
    internal const val AN_EVENT_ID = "\$vApgexcL8Vfh-WxYKsFKCDooo67ttbjm3TiVKXaWijU"

    internal val A_POLL_CONTENT = MessagePollContent(
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

    internal val A_POLL_RESPONSE_CONTENT = MessagePollResponseContent(
            unstableResponse = PollResponse(
                    answers = listOf("5ef5f7b0-c9a1-49cf-a0b3-374729a43e76")
            ),
            relatesTo = RelationDefaultContent(
                    type = RelationType.REFERENCE,
                    eventId = AN_EVENT_ID
            )
    )

    internal val A_POLL_END_CONTENT = MessageEndPollContent(
            relatesTo = RelationDefaultContent(
                    type = RelationType.REFERENCE,
                    eventId = AN_EVENT_ID
            )
    )

    internal val AN_INVALID_POLL_RESPONSE_CONTENT = MessagePollResponseContent(
            unstableResponse = PollResponse(
                    answers = listOf("fake-option-id")
            ),
            relatesTo = RelationDefaultContent(
                    type = RelationType.REFERENCE,
                    eventId = AN_EVENT_ID
            )
    )

    internal val A_POLL_START_EVENT = Event(
            type = EventType.POLL_START.unstable,
            eventId = AN_EVENT_ID,
            originServerTs = 1652435922563,
            senderId = A_USER_ID_1,
            roomId = A_ROOM_ID,
            content = A_POLL_CONTENT.toContent()
    )

    internal val A_POLL_RESPONSE_EVENT = Event(
            type = EventType.POLL_RESPONSE.unstable,
            eventId = AN_EVENT_ID,
            originServerTs = 1652435922563,
            senderId = A_USER_ID_1,
            roomId = A_ROOM_ID,
            content = A_POLL_RESPONSE_CONTENT.toContent()
    )

    internal val A_POLL_END_EVENT = Event(
            type = EventType.POLL_END.unstable,
            eventId = AN_EVENT_ID,
            originServerTs = 1652435922563,
            senderId = A_USER_ID_1,
            roomId = A_ROOM_ID,
            content = A_POLL_END_CONTENT.toContent()
    )

    internal val A_TIMELINE_EVENT = TimelineEvent(
            root = A_POLL_START_EVENT,
            localId = 1234,
            eventId = AN_EVENT_ID,
            displayIndex = 0,
            senderInfo = SenderInfo(A_USER_ID_1, "A_USER_ID_1", true, null)
    )

    internal val A_POLL_RESPONSE_EVENT_WITH_A_WRONG_REFERENCE = A_POLL_RESPONSE_EVENT.copy(
            content = A_POLL_RESPONSE_CONTENT
                    .copy(
                            relatesTo = RelationDefaultContent(
                                    type = RelationType.REPLACE,
                                    eventId = null
                            )
                    )
                    .toContent()
    )

    internal val A_POLL_REPLACE_EVENT = A_POLL_START_EVENT.copy(
            content = A_POLL_CONTENT
                    .copy(
                            relatesTo = RelationDefaultContent(
                                    type = RelationType.REPLACE,
                                    eventId = AN_EVENT_ID
                            )
                    )
                    .toContent()
    )

    internal val A_BROKEN_POLL_REPLACE_EVENT = A_POLL_START_EVENT.copy(
            content = A_POLL_CONTENT
                    .copy(
                            relatesTo = RelationDefaultContent(
                                    type = RelationType.REPLACE,
                                    eventId = null
                            )
                    )
                    .toContent()
    )

    internal val A_POLL_REFERENCE_EVENT = A_POLL_START_EVENT.copy(
            content = A_POLL_CONTENT
                    .copy(
                            relatesTo = RelationDefaultContent(
                                    type = RelationType.REFERENCE,
                                    eventId = AN_EVENT_ID
                            )
                    )
                    .toContent()
    )

    internal val AN_INVALID_POLL_RESPONSE_EVENT = A_POLL_RESPONSE_EVENT.copy(
            content = AN_INVALID_POLL_RESPONSE_CONTENT.toContent()
    )
}
