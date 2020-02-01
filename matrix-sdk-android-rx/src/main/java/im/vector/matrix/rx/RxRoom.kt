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

import im.vector.matrix.android.api.crypto.RoomEncryptionTrustLevel
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.members.RoomMemberQueryParams
import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.api.session.room.model.ReadReceipt
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.matrix.android.api.session.room.send.UserDraft
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber

class RxRoom(private val room: Room, private val session: Session) {

    fun liveRoomSummary(): Observable<Optional<RoomSummary>> {
        val summaryObservable = room.getRoomSummaryLive()
                .asObservable()
                .startWith(room.roomSummary().toOptional())
                .doOnNext { Timber.d("RX: summary emitted for: ${it.getOrNull()?.roomId}") }

        val memberIdsChangeObservable = summaryObservable
                .map {
                    it.getOrNull()?.let { roomSummary ->
                        if (roomSummary.isEncrypted) {
                            // Return the list of other users
                            roomSummary.otherMemberIds + listOf(session.myUserId)
                        } else {
                            // Return an empty list, the room is not encrypted
                            emptyList()
                        }
                    }.orEmpty()
                }.distinctUntilChanged()
                .doOnNext { Timber.d("RX: memberIds emitted. Size: ${it.size}") }

        // Observe the device info of the users in the room
        val cryptoDeviceInfoObservable = memberIdsChangeObservable
                .switchMap { membersIds ->
                    session.getLiveCryptoDeviceInfo(membersIds)
                            .asObservable()
                            .map {
                                // If any key change, emit the userIds list
                                membersIds
                            }
                            .startWith(membersIds)
                            .doOnNext { Timber.d("RX: CryptoDeviceInfo emitted. Size: ${it.size}") }
                }
                .doOnNext { Timber.d("RX: cryptoDeviceInfo emitted 2. Size: ${it.size}") }

        val roomEncryptionTrustLevelObservable = cryptoDeviceInfoObservable
                .map { userIds ->
                    if (userIds.isEmpty()) {
                        Optional<RoomEncryptionTrustLevel>(null)
                    } else {
                        session.getCrossSigningService().getTrustLevelForUsers(userIds).toOptional()
                    }
                }
                .doOnNext { Timber.d("RX: roomEncryptionTrustLevel emitted: ${it.getOrNull()?.name}") }

        return Observable
                .combineLatest<Optional<RoomSummary>, Optional<RoomEncryptionTrustLevel>, Optional<RoomSummary>>(
                        summaryObservable,
                        roomEncryptionTrustLevelObservable,
                        BiFunction { summary, level ->
                            summary.getOrNull()?.copy(
                                    roomEncryptionTrustLevel = level.getOrNull()
                            ).toOptional()
                        }
                )
                .doOnNext { Timber.d("RX: final room summary emitted for ${it.getOrNull()?.roomId}") }
    }

    fun liveRoomMembers(queryParams: RoomMemberQueryParams): Observable<List<RoomMemberSummary>> {
        val roomMembersObservable = room.getRoomMembersLive(queryParams).asObservable()
                .startWith(room.getRoomMembers(queryParams))
                .doOnNext { Timber.d("RX: room members emitted. Size: ${it.size}") }

        // TODO Do it only for room members of the room (switchMap)
        val cryptoDeviceInfoObservable = session.getLiveCryptoDeviceInfo().asObservable()
                .startWith(emptyList<CryptoDeviceInfo>())
                .doOnNext { Timber.d("RX: cryptoDeviceInfo emitted. Size: ${it.size}") }

        return Observable
                .combineLatest<List<RoomMemberSummary>, List<CryptoDeviceInfo>, List<RoomMemberSummary>>(
                        roomMembersObservable,
                        cryptoDeviceInfoObservable,
                        BiFunction { summaries, _ ->
                            summaries.map {
                                if (room.isEncrypted()) {
                                    it.copy(
                                            // Get the trust level of a virtual room with only this user
                                            userEncryptionTrustLevel = session.getCrossSigningService().getTrustLevelForUsers(listOf(it.userId))
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                )
                .doOnNext { Timber.d("RX: final room members emitted. Size: ${it.size}") }
    }

    fun liveAnnotationSummary(eventId: String): Observable<Optional<EventAnnotationsSummary>> {
        return room.getEventAnnotationsSummaryLive(eventId).asObservable()
                .startWith(room.getEventAnnotationsSummary(eventId).toOptional())
    }

    fun liveTimelineEvent(eventId: String): Observable<Optional<TimelineEvent>> {
        return room.getTimeLineEventLive(eventId).asObservable()
                .startWith(room.getTimeLineEvent(eventId).toOptional())
    }

    fun liveStateEvent(eventType: String): Observable<Optional<Event>> {
        return room.getStateEventLive(eventType).asObservable()
                .startWith(room.getStateEvent(eventType).toOptional())
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
}

fun Room.rx(session: Session): RxRoom {
    return RxRoom(this, session)
}
