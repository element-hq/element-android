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
import im.vector.matrix.android.api.session.room.members.ChangeMembershipState
import im.vector.matrix.android.internal.database.awaitNotEmptyResult
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.create.JoinRoomResponse
import im.vector.matrix.android.internal.session.room.membership.RoomChangeMembershipStateDataSource
import im.vector.matrix.android.internal.session.room.read.SetReadMarkersTask
import im.vector.matrix.android.internal.task.Task
import io.realm.RealmConfiguration
import kotlinx.coroutines.TimeoutCancellationException
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface JoinRoomTask : Task<JoinRoomTask.Params, Unit> {
    data class Params(
            val roomIdOrAlias: String,
            val reason: String?,
            val viaServers: List<String> = emptyList()
    )
}

internal class DefaultJoinRoomTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val readMarkersTask: SetReadMarkersTask,
        @SessionDatabase
        private val realmConfiguration: RealmConfiguration,
        private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource,
        private val eventBus: EventBus
) : JoinRoomTask {

    override suspend fun execute(params: JoinRoomTask.Params) {
        roomChangeMembershipStateDataSource.updateState(params.roomIdOrAlias, ChangeMembershipState.Joining)
        val joinRoomResponse = try {
            executeRequest<JoinRoomResponse>(eventBus) {
                apiCall = roomAPI.join(params.roomIdOrAlias, params.viaServers, mapOf("reason" to params.reason))
            }
        } catch (failure: Throwable) {
            roomChangeMembershipStateDataSource.updateState(params.roomIdOrAlias, ChangeMembershipState.FailedJoining(failure))
            throw failure
        }
        // Wait for room to come back from the sync (but it can maybe be in the DB is the sync response is received before)
        val roomId = joinRoomResponse.roomId
        try {
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomEntity::class.java)
                        .equalTo(RoomEntityFields.ROOM_ID, roomId)
            }
        } catch (exception: TimeoutCancellationException) {
            throw JoinRoomFailure.JoinedWithTimeout
        }
        setReadMarkers(roomId)
    }

    private suspend fun setReadMarkers(roomId: String) {
        val setReadMarkerParams = SetReadMarkersTask.Params(roomId, forceReadMarker = true, forceReadReceipt = true)
        readMarkersTask.execute(setReadMarkerParams)
    }
}
