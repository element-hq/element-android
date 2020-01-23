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
import io.reactivex.Observable
import io.reactivex.Single

class RxSession(private val session: Session) {

    fun liveRoomSummaries(queryParams: RoomSummaryQueryParams): Observable<List<RoomSummary>> {
        return session.getRoomSummariesLive(queryParams).asObservable()
                .startWithCallable {
                    session.getRoomSummaries(queryParams)
                }
    }

    fun liveGroupSummaries(queryParams: GroupSummaryQueryParams): Observable<List<GroupSummary>> {
        return session.getGroupSummariesLive(queryParams).asObservable()
                .startWithCallable {
                    session.getGroupSummaries(queryParams)
                }
    }

    fun liveBreadcrumbs(): Observable<List<RoomSummary>> {
        return session.getBreadcrumbsLive().asObservable()
                .startWithCallable {
                    session.getBreadcrumbs()
                }
    }

    fun liveSyncState(): Observable<SyncState> {
        return session.getSyncStateLive().asObservable()
    }

    fun livePushers(): Observable<List<Pusher>> {
        return session.getPushersLive().asObservable()
    }

    fun liveUser(userId: String): Observable<Optional<User>> {
        return session.getUserLive(userId).asObservable()
                .startWithCallable {
                    session.getUser(userId).toOptional()
                }
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
}

fun Session.rx(): RxSession {
    return RxSession(this)
}
