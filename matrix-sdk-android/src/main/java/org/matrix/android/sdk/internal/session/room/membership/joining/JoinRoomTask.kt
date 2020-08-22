/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.membership.joining

import org.matrix.android.sdk.api.session.room.failure.JoinRoomFailure
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.create.JoinRoomResponse
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.read.SetReadMarkersTask
import org.matrix.android.sdk.internal.task.Task
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
