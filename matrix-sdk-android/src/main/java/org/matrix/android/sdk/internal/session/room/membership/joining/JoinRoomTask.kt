/*
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

import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.session.room.failure.JoinRoomFailure
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.read.SetReadMarkersTask
import org.matrix.android.sdk.internal.task.Task
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
        private val globalErrorReceiver: GlobalErrorReceiver
) : JoinRoomTask {

    override suspend fun execute(params: JoinRoomTask.Params) {
        val currentState = roomChangeMembershipStateDataSource.getState(params.roomIdOrAlias)
        if (currentState.isInProgress() || currentState == ChangeMembershipState.Joined) {
            return
        }
        roomChangeMembershipStateDataSource.updateState(params.roomIdOrAlias, ChangeMembershipState.Joining)
        val joinRoomResponse = try {
            executeRequest(globalErrorReceiver) {
                roomAPI.join(
                        roomIdOrAlias = params.roomIdOrAlias,
                        viaServers = params.viaServers.take(3),
                        params = mapOf("reason" to params.reason)
                )
            }
        } catch (failure: Throwable) {
            roomChangeMembershipStateDataSource.updateState(params.roomIdOrAlias, ChangeMembershipState.FailedJoining(failure))
            throw failure
        }
        // Wait for room to come back from the sync (but it can maybe be in the DB is the sync response is received before)
        val roomId = joinRoomResponse.roomId
        try {
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomSummaryEntity::class.java)
                        .equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
                        .equalTo(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.JOIN.name)
            }
        } catch (exception: TimeoutCancellationException) {
            throw JoinRoomFailure.JoinedWithTimeout
        }

        Realm.getInstance(realmConfiguration).executeTransactionAsync {
            RoomSummaryEntity.where(it, roomId).findFirst()?.lastActivityTime = System.currentTimeMillis()
        }

        setReadMarkers(roomId)
    }

    private suspend fun setReadMarkers(roomId: String) {
        val setReadMarkerParams = SetReadMarkersTask.Params(roomId, forceReadMarker = true, forceReadReceipt = true)
        readMarkersTask.execute(setReadMarkerParams)
    }
}
