/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.rx

import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.api.session.room.model.ReadReceipt
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.matrix.android.api.session.room.send.UserDraft
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.Optional
import io.reactivex.Observable
import io.reactivex.Single

class RxRoom(private val room: Room) {

    fun liveRoomSummary(): Observable<Optional<RoomSummary>> {
        return room.getRoomSummaryLive().asObservable()
    }

    fun liveRoomMemberIds(): Observable<List<String>> {
        return room.getRoomMemberIdsLive().asObservable()
    }

    fun liveAnnotationSummary(eventId: String): Observable<Optional<EventAnnotationsSummary>> {
        return room.getEventSummaryLive(eventId).asObservable()
    }

    fun liveTimelineEvent(eventId: String): Observable<Optional<TimelineEvent>> {
        return room.getTimeLineEventLive(eventId).asObservable()
    }

    fun liveReadMarker(): Observable<Optional<String>> {
        return room.getReadMarkerLive().asObservable()
    }

    fun liveReadReceipt(): Observable<Optional<String>> {
        return room.getMyReadReceiptLive().asObservable()
    }

    fun loadRoomMembersIfNeeded(): Single<Unit> = Single.create {
        room.loadRoomMembersIfNeeded(MatrixCallbackSingle(it)).toSingle(it)
    }

    fun joinRoom(reason: String?, viaServers: List<String> = emptyList()): Single<Unit> = Single.create {
        room.join(reason, viaServers, MatrixCallbackSingle(it)).toSingle(it)
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
}

fun Room.rx(): RxRoom {
    return RxRoom(this)
}
