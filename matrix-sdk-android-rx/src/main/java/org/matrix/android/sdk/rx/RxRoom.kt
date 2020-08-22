/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.rx

import android.net.Uri
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class RxRoom(private val room: Room) {

    fun liveRoomSummary(): Observable<Optional<RoomSummary>> {
        return room.getRoomSummaryLive()
                .asObservable()
                .startWithCallable { room.roomSummary().toOptional() }
    }

    fun liveRoomMembers(queryParams: RoomMemberQueryParams): Observable<List<RoomMemberSummary>> {
        return room.getRoomMembersLive(queryParams).asObservable()
                .startWithCallable {
                    room.getRoomMembers(queryParams)
                }
    }

    fun liveAnnotationSummary(eventId: String): Observable<Optional<EventAnnotationsSummary>> {
        return room.getEventAnnotationsSummaryLive(eventId).asObservable()
                .startWithCallable {
                    room.getEventAnnotationsSummary(eventId).toOptional()
                }
    }

    fun liveTimelineEvent(eventId: String): Observable<Optional<TimelineEvent>> {
        return room.getTimeLineEventLive(eventId).asObservable()
                .startWithCallable {
                    room.getTimeLineEvent(eventId).toOptional()
                }
    }

    fun liveStateEvent(eventType: String, stateKey: QueryStringValue): Observable<Optional<Event>> {
        return room.getStateEventLive(eventType, stateKey).asObservable()
                .startWithCallable {
                    room.getStateEvent(eventType, stateKey).toOptional()
                }
    }

    fun liveStateEvents(eventTypes: Set<String>): Observable<List<Event>> {
        return room.getStateEventsLive(eventTypes).asObservable()
                .startWithCallable {
                    room.getStateEvents(eventTypes)
                }
    }

    fun liveReadMarker(): Observable<Optional<String>> {
        return room.getReadMarkerLive().asObservable()
    }

    fun liveReadReceipt(): Observable<Optional<String>> {
        return room.getMyReadReceiptLive().asObservable()
    }

    fun loadRoomMembersIfNeeded(): Single<Unit> = singleBuilder {
        room.loadRoomMembersIfNeeded(it)
    }

    fun joinRoom(reason: String? = null,
                 viaServers: List<String> = emptyList()): Single<Unit> = singleBuilder {
        room.join(reason, viaServers, it)
    }

    fun liveEventReadReceipts(eventId: String): Observable<List<ReadReceipt>> {
        return room.getEventReadReceiptsLive(eventId).asObservable()
    }

    fun liveDrafts(): Observable<List<UserDraft>> {
        return room.getDraftsLive().asObservable()
    }

    fun liveNotificationState(): Observable<RoomNotificationState> {
        return room.getLiveRoomNotificationState().asObservable()
    }

    fun invite(userId: String, reason: String? = null): Completable = completableBuilder<Unit> {
        room.invite(userId, reason, it)
    }

    fun invite3pid(threePid: ThreePid): Completable = completableBuilder<Unit> {
        room.invite3pid(threePid, it)
    }

    fun updateTopic(topic: String): Completable = completableBuilder<Unit> {
        room.updateTopic(topic, it)
    }

    fun updateName(name: String): Completable = completableBuilder<Unit> {
        room.updateName(name, it)
    }

    fun addRoomAlias(alias: String): Completable = completableBuilder<Unit> {
        room.addRoomAlias(alias, it)
    }

    fun updateCanonicalAlias(alias: String): Completable = completableBuilder<Unit> {
        room.updateCanonicalAlias(alias, it)
    }

    fun updateHistoryReadability(readability: RoomHistoryVisibility): Completable = completableBuilder<Unit> {
        room.updateHistoryReadability(readability, it)
    }

    fun updateAvatar(avatarUri: Uri, fileName: String): Completable = completableBuilder<Unit> {
        room.updateAvatar(avatarUri, fileName, it)
    }
}

fun Room.rx(): RxRoom {
    return RxRoom(this)
}
