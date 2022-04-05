/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.flow

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional

typealias ThreadRootEvent = TimelineEvent

class FlowRoom(private val room: Room) {

    fun liveRoomSummary(): Flow<Optional<RoomSummary>> {
        return room.getRoomSummaryLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.roomSummary().toOptional()
                }
    }

    fun liveRoomMembers(queryParams: RoomMemberQueryParams): Flow<List<RoomMemberSummary>> {
        return room.getRoomMembersLive(queryParams).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getRoomMembers(queryParams)
                }
    }

    fun liveAnnotationSummary(eventId: String): Flow<Optional<EventAnnotationsSummary>> {
        return room.getEventAnnotationsSummaryLive(eventId).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getEventAnnotationsSummary(eventId).toOptional()
                }
    }

    fun liveTimelineEvent(eventId: String): Flow<Optional<TimelineEvent>> {
        return room.getTimelineEventLive(eventId).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getTimelineEvent(eventId).toOptional()
                }
    }

    fun liveStateEvent(eventType: String, stateKey: QueryStringValue): Flow<Optional<Event>> {
        return room.getStateEventLive(eventType, stateKey).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getStateEvent(eventType, stateKey).toOptional()
                }
    }

    fun liveStateEvents(eventTypes: Set<String>): Flow<List<Event>> {
        return room.getStateEventsLive(eventTypes).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getStateEvents(eventTypes)
                }
    }

    fun liveReadMarker(): Flow<Optional<String>> {
        return room.getReadMarkerLive().asFlow()
    }

    fun liveReadReceipt(): Flow<Optional<String>> {
        return room.getMyReadReceiptLive().asFlow()
    }

    fun liveEventReadReceipts(eventId: String): Flow<List<ReadReceipt>> {
        return room.getEventReadReceiptsLive(eventId).asFlow()
    }

    fun liveDraft(): Flow<Optional<UserDraft>> {
        return room.getDraftLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getDraft().toOptional()
                }
    }

    fun liveNotificationState(): Flow<RoomNotificationState> {
        return room.getLiveRoomNotificationState().asFlow()
    }

    fun liveThreadSummaries(): Flow<List<ThreadSummary>> {
        return room.getAllThreadSummariesLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getAllThreadSummaries()
                }
    }
    fun liveThreadList(): Flow<List<ThreadRootEvent>> {
        return room.getAllThreadsLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getAllThreads()
                }
    }
    fun liveLocalUnreadThreadList(): Flow<List<ThreadRootEvent>> {
        return room.getMarkedThreadNotificationsLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getMarkedThreadNotifications()
                }
    }
}

fun Room.flow(): FlowRoom {
    return FlowRoom(this)
}
