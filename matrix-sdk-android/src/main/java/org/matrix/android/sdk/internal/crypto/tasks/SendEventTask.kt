/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.create.CreateRoomFromLocalRoomTask
import org.matrix.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface SendEventTask : Task<SendEventTask.Params, String> {
    data class Params(
            val event: Event,
            val encrypt: Boolean
    )
}

internal class DefaultSendEventTask @Inject constructor(
        private val localEchoRepository: LocalEchoRepository,
        private val encryptEventTask: EncryptEventTask,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val createRoomFromLocalRoomTask: CreateRoomFromLocalRoomTask,
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SendEventTask {

    override suspend fun execute(params: SendEventTask.Params): String {
        try {
            if (params.event.isLocalRoomEvent) {
                return createRoomAndSendEvent(params)
            }

            // Make sure to load all members in the room before sending the event.
            params.event.roomId
                    ?.takeIf { params.encrypt }
                    ?.let { roomId ->
                        try {
                            loadRoomMembersTask.execute(LoadRoomMembersTask.Params(roomId))
                        } catch (failure: Throwable) {
                            // send any way?
                            // the result is that some users won't probably be able to decrypt :/
                            Timber.w(failure, "SendEvent: failed to load members in room ${params.event.roomId}")
                        }
                    }

            val event = handleEncryption(params)
            val localId = event.eventId!!
            localEchoRepository.updateSendState(localId, params.event.roomId, SendState.SENDING)
            val response = executeRequest(globalErrorReceiver) {
                roomAPI.send(
                        localId,
                        roomId = event.roomId ?: "",
                        content = event.content,
                        eventType = event.type ?: ""
                )
            }
            Timber.d("Event sent to ${event.roomId} with event id ${response.eventId}")
            localEchoRepository.updateSendState(localId, params.event.roomId, SendState.SENT)
            return response.eventId.also {
                Timber.d("Event: $it just sent in ${params.event.roomId}")
            }
        } catch (e: Throwable) {
//            localEchoRepository.updateSendState(params.event.eventId!!, SendState.UNDELIVERED)
            Timber.w(e, "Unable to send the Event")
            throw e
        }
    }

    private suspend fun createRoomAndSendEvent(params: SendEventTask.Params): String {
        val roomId = createRoomFromLocalRoomTask.execute(CreateRoomFromLocalRoomTask.Params(params.event.roomId.orEmpty()))
        Timber.d("State event: convert local room (${params.event.roomId}) to existing room ($roomId) before sending the event.")
        return execute(params.copy(event = params.event.copy(roomId = roomId)))
    }

    @Throws
    private suspend fun handleEncryption(params: SendEventTask.Params): Event {
        if (params.encrypt && !params.event.isEncrypted()) {
            return encryptEventTask.execute(
                    EncryptEventTask.Params(
                            params.event.roomId ?: "",
                            params.event,
                            listOf("m.relates_to")
                    )
            )
        }
        return params.event
    }

    private val Event.isLocalRoomEvent
        get() = RoomLocalEcho.isLocalEchoId(roomId.orEmpty())
}
