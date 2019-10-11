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

package im.vector.matrix.android.internal.session.room.membership.joining

import im.vector.matrix.android.api.session.room.failure.JoinRoomFailure
import im.vector.matrix.android.internal.database.RealmQueryLatch
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.read.SetReadMarkersTask
import im.vector.matrix.android.internal.task.Task
import io.realm.RealmConfiguration
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface JoinRoomTask : Task<JoinRoomTask.Params, Unit> {
    data class Params(
            val roomId: String,
            val viaServers: List<String> = emptyList()
    )
}

internal class DefaultJoinRoomTask @Inject constructor(private val roomAPI: RoomAPI,
                                                       private val readMarkersTask: SetReadMarkersTask,
                                                       @SessionDatabase
                                                       private val realmConfiguration: RealmConfiguration) : JoinRoomTask {

    override suspend fun execute(params: JoinRoomTask.Params) {
        executeRequest<Unit> {
            apiCall = roomAPI.join(params.roomId, params.viaServers, HashMap())
        }
        val roomId = params.roomId
        // Wait for room to come back from the sync (but it can maybe be in the DB is the sync response is received before)
        val rql = RealmQueryLatch<RoomEntity>(realmConfiguration) { realm ->
            realm.where(RoomEntity::class.java)
                    .equalTo(RoomEntityFields.ROOM_ID, roomId)
        }
        try {
            rql.await(timeout = 1L, timeUnit = TimeUnit.MINUTES)
        } catch (exception: Exception) {
            throw JoinRoomFailure.JoinedWithTimeout
        }
        setReadMarkers(roomId)
    }

    private suspend fun setReadMarkers(roomId: String) {
        val setReadMarkerParams = SetReadMarkersTask.Params(roomId, markAllAsRead = true)
        readMarkersTask.execute(setReadMarkerParams)
    }
}
