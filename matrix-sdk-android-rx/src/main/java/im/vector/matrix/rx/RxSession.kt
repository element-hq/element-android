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
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.api.util.Optional
import io.reactivex.Observable
import io.reactivex.Single

class RxSession(private val session: Session) {

    fun liveRoomSummaries(): Observable<List<RoomSummary>> {
        return session.liveRoomSummaries().asObservable()
    }

    fun liveGroupSummaries(): Observable<List<GroupSummary>> {
        return session.liveGroupSummaries().asObservable()
    }

    fun liveSyncState(): Observable<SyncState> {
        return session.syncState().asObservable()
    }

    fun livePushers(): Observable<List<Pusher>> {
        return session.livePushers().asObservable()
    }

    fun liveUser(userId: String): Observable<Optional<User>> {
        return session.liveUser(userId).asObservable().distinctUntilChanged()
    }

    fun liveUsers(): Observable<List<User>> {
        return session.liveUsers().asObservable()
    }

    fun liveIgnoredUsers(): Observable<List<User>> {
        return session.liveIgnoredUsers().asObservable()
    }

    fun livePagedUsers(filter: String? = null): Observable<PagedList<User>> {
        return session.livePagedUsers(filter).asObservable()
    }

    fun createRoom(roomParams: CreateRoomParams): Single<String> = Single.create {
        session.createRoom(roomParams, MatrixCallbackSingle(it)).toSingle(it)
    }

    fun searchUsersDirectory(search: String,
                             limit: Int,
                             excludedUserIds: Set<String>): Single<List<User>> = Single.create {
        session.searchUsersDirectory(search, limit, excludedUserIds, MatrixCallbackSingle(it)).toSingle(it)
    }

    fun joinRoom(roomId: String, viaServers: List<String> = emptyList()): Single<Unit> = Single.create {
        session.joinRoom(roomId, viaServers, MatrixCallbackSingle(it)).toSingle(it)
    }
}

fun Session.rx(): RxSession {
    return RxSession(this)
}
