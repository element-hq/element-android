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

package im.vector.matrix.android.internal.session.room.create

import arrow.core.Try
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.model.create.CreateRoomResponse
import im.vector.matrix.android.internal.database.RealmQueryLatch
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task
import io.realm.RealmConfiguration
import javax.inject.Inject

internal interface CreateRoomTask : Task<CreateRoomParams, String>

@SessionScope
internal class DefaultCreateRoomTask @Inject constructor(private val roomAPI: RoomAPI,
                                     private val realmConfiguration: RealmConfiguration) : CreateRoomTask {


    override suspend fun execute(params: CreateRoomParams): Try<String> {
        return executeRequest<CreateRoomResponse> {
            apiCall = roomAPI.createRoom(params)
        }.flatMap { createRoomResponse ->
            val roomId = createRoomResponse.roomId!!

            // TODO Maybe do the same code for join room request ?
            // Wait for room to come back from the sync (but it can maybe be in the DB is the sync response is received before)
            val rql = RealmQueryLatch<RoomEntity>(realmConfiguration) { realm ->
                realm.where(RoomEntity::class.java)
                        .equalTo(RoomEntityFields.ROOM_ID, roomId)
            }

            rql.await()

            return Try.just(roomId)
        }
    }
}
