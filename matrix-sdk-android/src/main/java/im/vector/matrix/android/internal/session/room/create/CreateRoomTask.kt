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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.failure.CreateRoomFailure
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.model.create.CreateRoomResponse
import im.vector.matrix.android.internal.database.RealmQueryLatch
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.read.SetReadMarkersTask
import im.vector.matrix.android.internal.session.user.accountdata.DirectChatsHelper
import im.vector.matrix.android.internal.session.user.accountdata.UpdateUserAccountDataTask
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.RealmConfiguration
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface CreateRoomTask : Task<CreateRoomParams, String>

internal class DefaultCreateRoomTask @Inject constructor(private val roomAPI: RoomAPI,
                                                         private val monarchy: Monarchy,
                                                         private val directChatsHelper: DirectChatsHelper,
                                                         private val updateUserAccountDataTask: UpdateUserAccountDataTask,
                                                         private val readMarkersTask: SetReadMarkersTask,
                                                         @SessionDatabase
                                                         private val realmConfiguration: RealmConfiguration) : CreateRoomTask {

    override suspend fun execute(params: CreateRoomParams): String {
        val createRoomResponse = executeRequest<CreateRoomResponse> {
            apiCall = roomAPI.createRoom(params)
        }
        val roomId = createRoomResponse.roomId!!
        // Wait for room to come back from the sync (but it can maybe be in the DB is the sync response is received before)
        val rql = RealmQueryLatch<RoomEntity>(realmConfiguration) { realm ->
            realm.where(RoomEntity::class.java)
                    .equalTo(RoomEntityFields.ROOM_ID, roomId)
        }
        try {
            rql.await(timeout = 1L, timeUnit = TimeUnit.MINUTES)
        } catch (exception: Exception) {
            throw CreateRoomFailure.CreatedWithTimeout
        }
        if (params.isDirect()) {
            handleDirectChatCreation(params, roomId)
        }
        setReadMarkers(roomId)
        return roomId
    }

    private suspend fun handleDirectChatCreation(params: CreateRoomParams, roomId: String) {
        val otherUserId = params.getFirstInvitedUserId()
                ?: throw IllegalStateException("You can't create a direct room without an invitedUser")

        monarchy.awaitTransaction { realm ->
            RoomSummaryEntity.where(realm, roomId).findFirst()?.apply {
                this.directUserId = otherUserId
                this.isDirect = true
            }
        }
        val directChats = directChatsHelper.getLocalUserAccount()
        updateUserAccountDataTask.execute(UpdateUserAccountDataTask.DirectChatParams(directMessages = directChats))
    }

    private suspend fun setReadMarkers(roomId: String) {
        val setReadMarkerParams = SetReadMarkersTask.Params(roomId, markAllAsRead = true)
        return readMarkersTask.execute(setReadMarkerParams)
    }
}
