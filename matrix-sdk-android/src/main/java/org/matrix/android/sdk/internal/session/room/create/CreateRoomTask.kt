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

package org.matrix.android.sdk.internal.session.room.create

import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.room.alias.RoomAliasError
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.alias.RoomAliasAvailabilityChecker
import org.matrix.android.sdk.internal.session.room.read.SetReadMarkersTask
import org.matrix.android.sdk.internal.session.user.accountdata.DirectChatsHelper
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateUserAccountDataTask
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal interface CreateRoomTask : Task<CreateRoomParams, String>

internal class DefaultCreateRoomTask @Inject constructor(
        private val roomAPI: RoomAPI,
        @SessionDatabase private val realmInstance: RealmInstance,
        private val aliasAvailabilityChecker: RoomAliasAvailabilityChecker,
        private val directChatsHelper: DirectChatsHelper,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val readMarkersTask: SetReadMarkersTask,
        private val createRoomBodyBuilder: CreateRoomBodyBuilder,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val clock: Clock,
) : CreateRoomTask {

    override suspend fun execute(params: CreateRoomParams): String {
        if (params.preset == CreateRoomPreset.PRESET_PUBLIC_CHAT) {
            try {
                aliasAvailabilityChecker.check(params.roomAliasName)
            } catch (aliasError: RoomAliasError) {
                throw CreateRoomFailure.AliasError(aliasError)
            }
        }

        val createRoomBody = createRoomBodyBuilder.build(params)

        val createRoomResponse = try {
            executeRequest(globalErrorReceiver) {
                roomAPI.createRoom(createRoomBody)
            }
        } catch (throwable: Throwable) {
            if (throwable is Failure.ServerError) {
                if (throwable.httpCode == 403 &&
                        throwable.error.code == MatrixError.M_FORBIDDEN &&
                        throwable.error.message.startsWith("Federation denied with")) {
                    throw CreateRoomFailure.CreatedWithFederationFailure(throwable.error)
                } else if (throwable.httpCode == 400 &&
                        throwable.error.code == MatrixError.M_UNKNOWN &&
                        throwable.error.message == "Invalid characters in room alias") {
                    throw CreateRoomFailure.AliasError(RoomAliasError.AliasInvalid)
                }
            }
            throw throwable
        }
        val roomId = createRoomResponse.roomId
        // Wait for room to come back from the sync (but it can maybe be in the DB if the sync response is received before)
        try {
            awaitNotEmptyResult(realmInstance, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                RoomSummaryEntity.where(realm, roomId)
                        .query("membershipStr == $0", Membership.JOIN.name)
            }
        } catch (exception: TimeoutCancellationException) {
            throw CreateRoomFailure.CreatedWithTimeout(roomId)
        }

        realmInstance.write {
            RoomSummaryEntity.where(this, roomId).first().find()?.lastActivityTime = clock.epochMillis()
        }

        handleDirectChatCreation(roomId, createRoomBody.getDirectUserId())
        setReadMarkers(roomId)
        return roomId
    }

    private suspend fun handleDirectChatCreation(roomId: String, otherUserId: String?) {
        otherUserId ?: return // This is not a direct room
        realmInstance.write {
            RoomSummaryEntity.where(this, roomId).first().find()?.apply {
                this.directUserId = otherUserId
                this.isDirect = true
            }
        }
        val directChats = directChatsHelper.getLocalDirectMessages()
        updateUserAccountDataTask.execute(UpdateUserAccountDataTask.DirectChatParams(directMessages = directChats))
    }

    private suspend fun setReadMarkers(roomId: String) {
        val setReadMarkerParams = SetReadMarkersTask.Params(roomId, forceReadReceipt = true, forceReadMarker = true)
        return readMarkersTask.execute(setReadMarkerParams)
    }
}
