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
import org.matrix.android.sdk.api.query.QueryStateEventValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getStateEvent
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.LocalRoomSummary
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.session.room.send.UserDraft
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

    fun liveLocalRoomSummary(): Flow<Optional<LocalRoomSummary>> {
        return room.getLocalRoomSummaryLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.localRoomSummary().toOptional()
                }
    }

    fun liveRoomMembers(queryParams: RoomMemberQueryParams): Flow<List<RoomMemberSummary>> {
        return room.membershipService().getRoomMembersLive(queryParams).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.membershipService().getRoomMembers(queryParams)
                }
    }

    fun liveAreAllMembersLoaded(): Flow<Boolean> {
        return room.membershipService().areAllMembersLoadedLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.membershipService().areAllMembersLoaded()
                }
    }

    fun liveAnnotationSummary(eventId: String): Flow<Optional<EventAnnotationsSummary>> {
        return room.relationService().getEventAnnotationsSummaryLive(eventId).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.relationService().getEventAnnotationsSummary(eventId).toOptional()
                }
    }

    fun liveTimelineEvent(eventId: String): Flow<Optional<TimelineEvent>> {
        return room.timelineService().getTimelineEventLive(eventId).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getTimelineEvent(eventId).toOptional()
                }
    }

    fun liveStateEvent(eventType: String, stateKey: QueryStateEventValue): Flow<Optional<Event>> {
        return room.stateService().getStateEventLive(eventType, stateKey).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.getStateEvent(eventType, stateKey).toOptional()
                }
    }

    fun liveStateEvents(eventTypes: Set<String>, stateKey: QueryStateEventValue): Flow<List<Event>> {
        return room.stateService().getStateEventsLive(eventTypes, stateKey).asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.stateService().getStateEvents(eventTypes, stateKey)
                }
    }

    fun liveReadMarker(): Flow<Optional<String>> {
        return room.readService().getReadMarkerLive().asFlow()
    }

    fun liveReadReceipt(threadId: String?): Flow<Optional<String>> {
        return room.readService().getMyReadReceiptLive(threadId).asFlow()
    }

    fun liveEventReadReceipts(eventId: String): Flow<List<ReadReceipt>> {
        return room.readService().getEventReadReceiptsLive(eventId).asFlow()
    }

    fun liveDraft(): Flow<Optional<UserDraft>> {
        return room.draftService().getDraftLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.draftService().getDraft().toOptional()
                }
    }

    fun liveNotificationState(): Flow<RoomNotificationState> {
        return room.roomPushRuleService().getLiveRoomNotificationState().asFlow()
    }

    fun liveThreadList(): Flow<List<ThreadRootEvent>> {
        return room.threadsLocalService().getAllThreadsLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.threadsLocalService().getAllThreads()
                }
    }

    fun liveLocalUnreadThreadList(): Flow<List<ThreadRootEvent>> {
        return room.threadsLocalService().getMarkedThreadNotificationsLive().asFlow()
                .startWith(room.coroutineDispatchers.io) {
                    room.threadsLocalService().getMarkedThreadNotifications()
                }
    }
}

fun Room.flow(): FlowRoom {
    return FlowRoom(this)
}
