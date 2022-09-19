/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import com.zhuinden.monarchy.Monarchy
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.LocalRoomCreationState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereRoomId
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.task.Task
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Create a room on the server from a local room.
 * The configuration of the local room will be use to configure the new room.
 * The potential local room members will also be invited to this new room.
 */
internal interface CreateRoomFromLocalRoomTask : Task<CreateRoomFromLocalRoomTask.Params, String> {
    data class Params(val localRoomId: String)
}

internal class DefaultCreateRoomFromLocalRoomTask @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val createRoomTask: CreateRoomTask,
        private val roomSummaryDataSource: RoomSummaryDataSource,
) : CreateRoomFromLocalRoomTask {

    private val realmConfiguration
        get() = monarchy.realmConfiguration

    override suspend fun execute(params: CreateRoomFromLocalRoomTask.Params): String {
        val localRoomSummary = roomSummaryDataSource.getLocalRoomSummary(params.localRoomId)
                ?: error("## CreateRoomFromLocalRoomTask - Cannot retrieve LocalRoomSummary with roomId ${params.localRoomId}")

        // If a room has already been created for the given local room, return the existing roomId
        if (localRoomSummary.replacementRoomId != null) {
            return localRoomSummary.replacementRoomId
        }

        if (localRoomSummary.createRoomParams != null && localRoomSummary.roomSummary != null) {
            return createRoom(params.localRoomId, localRoomSummary.roomSummary, localRoomSummary.createRoomParams)
        } else {
            error("## CreateRoomFromLocalRoomTask - Invalid LocalRoomSummary: $localRoomSummary")
        }
    }

    /**
     * Create a room on the server for the given local room.
     *
     * @param localRoomId the local room identifier.
     * @param localRoomSummary the RoomSummary of the local room.
     * @param createRoomParams the CreateRoomParams object which was used to configure the local room.
     *
     * @return the identifier of the created room.
     */
    private suspend fun createRoom(localRoomId: String, localRoomSummary: RoomSummary, createRoomParams: CreateRoomParams): String {
        updateCreationState(localRoomId, LocalRoomCreationState.CREATING)
        val replacementRoomId = runCatching {
            createRoomTask.execute(createRoomParams)
        }.fold(
                { it },
                {
                    updateCreationState(localRoomId, LocalRoomCreationState.FAILURE)
                    throw it
                }
        )
        updateReplacementRoomId(localRoomId, replacementRoomId)
        waitForRoomEvents(replacementRoomId, localRoomSummary)
        updateCreationState(localRoomId, LocalRoomCreationState.CREATED)
        return replacementRoomId
    }

    /**
     * Wait for all the room events before triggering the created state.
     *
     * @param replacementRoomId the identifier of the created room
     * @param localRoomSummary the RoomSummary of the local room.
     */
    private suspend fun waitForRoomEvents(replacementRoomId: String, localRoomSummary: RoomSummary) {
        try {
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomSummaryEntity::class.java)
                        .equalTo(RoomSummaryEntityFields.ROOM_ID, replacementRoomId)
                        .equalTo(RoomSummaryEntityFields.INVITED_MEMBERS_COUNT, localRoomSummary.invitedMembersCount)
            }
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                EventEntity.whereRoomId(realm, replacementRoomId)
                        .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_HISTORY_VISIBILITY)
            }
            if (localRoomSummary.isEncrypted) {
                awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                    EventEntity.whereRoomId(realm, replacementRoomId)
                            .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_ENCRYPTION)
                }
            }
        } catch (exception: TimeoutCancellationException) {
            updateCreationState(localRoomSummary.roomId, LocalRoomCreationState.FAILURE)
            throw CreateRoomFailure.CreatedWithTimeout(replacementRoomId)
        }
    }

    private fun updateCreationState(roomId: String, creationState: LocalRoomCreationState) {
        monarchy.runTransactionSync { realm ->
            LocalRoomSummaryEntity.where(realm, roomId).findFirst()?.creationState = creationState
        }
    }

    private fun updateReplacementRoomId(localRoomId: String, replacementRoomId: String) {
        monarchy.runTransactionSync { realm ->
            LocalRoomSummaryEntity.where(realm, localRoomId).findFirst()?.replacementRoomId = replacementRoomId
        }
    }
}
