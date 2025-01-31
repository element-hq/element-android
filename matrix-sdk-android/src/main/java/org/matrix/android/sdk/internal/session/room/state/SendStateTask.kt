/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.state

import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.create.CreateRoomFromLocalRoomTask
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface SendStateTask : Task<SendStateTask.Params, String> {
    data class Params(
            val roomId: String,
            val stateKey: String,
            val eventType: String,
            val body: JsonDict
    )
}

internal class DefaultSendStateTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val createRoomFromLocalRoomTask: CreateRoomFromLocalRoomTask,
) : SendStateTask {

    override suspend fun execute(params: SendStateTask.Params): String {
        return executeRequest(globalErrorReceiver) {
            if (RoomLocalEcho.isLocalEchoId(params.roomId)) {
                // Room is local, so create a real one and send the event to this new room
                createRoomAndSendEvent(params)
            } else {
                val response = if (params.stateKey.isEmpty()) {
                    roomAPI.sendStateEvent(
                            roomId = params.roomId,
                            stateEventType = params.eventType,
                            params = params.body
                    )
                } else {
                    roomAPI.sendStateEvent(
                            roomId = params.roomId,
                            stateEventType = params.eventType,
                            stateKey = params.stateKey,
                            params = params.body
                    )
                }
                response.eventId.also {
                    Timber.d("State event: $it just sent in room ${params.roomId}")
                }
            }
        }
    }

    private suspend fun createRoomAndSendEvent(params: SendStateTask.Params): String {
        val roomId = createRoomFromLocalRoomTask.execute(CreateRoomFromLocalRoomTask.Params(params.roomId))
        Timber.d("State event: convert local room (${params.roomId}) to existing room ($roomId) before sending the event.")
        return execute(params.copy(roomId = roomId))
    }
}
