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

import androidx.paging.PagedList
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.api.session.group.GroupSummaryQueryParams
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.room.RoomSummaryQueryParams
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber

class RxSession(private val session: Session) {

    fun liveRoomSummaries(queryParams: RoomSummaryQueryParams): Observable<List<RoomSummary>> {
        val summariesObservable = session.getRoomSummariesLive(queryParams).asObservable()
                .startWith(session.getRoomSummaries(queryParams))
                .doOnNext { Timber.d("RX: summaries emitted: size: ${it.size}") }

        val cryptoDeviceInfoObservable = session.getLiveCryptoDeviceInfo().asObservable()
                .startWith(emptyList<CryptoDeviceInfo>())
                .doOnNext { Timber.d("RX: crypto device info emitted: size: ${it.size}") }

        return Observable
                .combineLatest<List<RoomSummary>, List<CryptoDeviceInfo>, List<RoomSummary>>(
                        summariesObservable,
                        cryptoDeviceInfoObservable,
                        BiFunction { summaries, _ ->
                            summaries.map {
                                if (it.isEncrypted) {
                                    it.copy(
                                            roomEncryptionTrustLevel = session.getCrossSigningService().getTrustLevelForUsers(it.otherMemberIds + listOf(session.myUserId))
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                )
                .doOnNext { Timber.d("RX: final summaries emitted: size: ${it.size}") }
    }

    fun liveGroupSummaries(queryParams: GroupSummaryQueryParams): Observable<List<GroupSummary>> {
        return session.getGroupSummariesLive(queryParams).asObservable()
                .startWith(session.getGroupSummaries(queryParams))
    }

    fun liveBreadcrumbs(): Observable<List<RoomSummary>> {
        return session.getBreadcrumbsLive().asObservable()
                .startWith(session.getBreadcrumbs())
    }

    fun liveSyncState(): Observable<SyncState> {
        return session.getSyncStateLive().asObservable()
    }

    fun livePushers(): Observable<List<Pusher>> {
        return session.getPushersLive().asObservable()
    }

    fun liveUser(userId: String): Observable<Optional<User>> {
        return session.getUserLive(userId).asObservable()
                .startWith(session.getUser(userId).toOptional())
    }

    fun liveUsers(): Observable<List<User>> {
        return session.getUsersLive().asObservable()
    }

    fun liveIgnoredUsers(): Observable<List<User>> {
        return session.getIgnoredUsersLive().asObservable()
    }

    fun livePagedUsers(filter: String? = null): Observable<PagedList<User>> {
        return session.getPagedUsersLive(filter).asObservable()
    }

    fun createRoom(roomParams: CreateRoomParams): Single<String> = singleBuilder {
        session.createRoom(roomParams, it)
    }

    fun searchUsersDirectory(search: String,
                             limit: Int,
                             excludedUserIds: Set<String>): Single<List<User>> = singleBuilder {
        session.searchUsersDirectory(search, limit, excludedUserIds, it)
    }

    fun joinRoom(roomId: String,
                 reason: String? = null,
                 viaServers: List<String> = emptyList()): Single<Unit> = singleBuilder {
        session.joinRoom(roomId, reason, viaServers, it)
    }

    fun getRoomIdByAlias(roomAlias: String,
                         searchOnServer: Boolean): Single<Optional<String>> = singleBuilder {
        session.getRoomIdByAlias(roomAlias, searchOnServer, it)
    }

    fun getProfileInfo(userId: String): Single<JsonDict> = singleBuilder {
        session.getProfile(userId, it)
    }

    fun liveUserCryptoDevices(userId: String): Observable<List<CryptoDeviceInfo>> {
        return session.getLiveCryptoDeviceInfo(userId).asObservable()
    }

    fun liveCrossSigningInfo(userId: String): Observable<Optional<MXCrossSigningInfo>> {
        return session.getCrossSigningService().getLiveCrossSigningKeys(userId).asObservable()
                .startWith(session.getCrossSigningService().getUserCrossSigningKeys(userId).toOptional())
    }
}

fun Session.rx(): RxSession {
    return RxSession(this)
}
