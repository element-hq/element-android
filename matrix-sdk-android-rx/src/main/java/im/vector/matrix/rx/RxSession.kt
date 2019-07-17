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

import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.model.User
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class RxSession(private val session: Session) {

    fun liveRoomSummaries(): Observable<List<RoomSummary>> {
        return session.liveRoomSummaries().asObservable().observeOn(Schedulers.computation())
    }

    fun liveGroupSummaries(): Observable<List<GroupSummary>> {
        return session.liveGroupSummaries().asObservable().observeOn(Schedulers.computation())
    }

    fun liveSyncState(): Observable<SyncState> {
        return session.syncState().asObservable().observeOn(Schedulers.computation())
    }

    fun livePushers(): Observable<List<Pusher>> {
        return session.livePushers().asObservable().observeOn(Schedulers.computation())
    }

    fun liveUsers(): Observable<List<User>> {
        return session.liveUsers().asObservable().observeOn(Schedulers.computation())
    }

}

fun Session.rx(): RxSession {
    return RxSession(this)
}