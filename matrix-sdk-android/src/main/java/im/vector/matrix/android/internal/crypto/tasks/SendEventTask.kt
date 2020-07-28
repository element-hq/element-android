/*
 * Copyright (c) 2020 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.tasks

import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.send.LocalEchoRepository
import im.vector.matrix.android.internal.session.room.send.SendResponse
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface SendEventTask : Task<SendEventTask.Params, String> {
    data class Params(
            val event: Event,
            val cryptoService: CryptoService?
    )
}

internal class DefaultSendEventTask @Inject constructor(
        private val localEchoRepository: LocalEchoRepository,
        private val encryptEventTask: DefaultEncryptEventTask,
        private val roomAPI: RoomAPI,
        private val eventBus: EventBus) : SendEventTask {

    override suspend fun execute(params: SendEventTask.Params): String {
        val event = handleEncryption(params)
        val localId = event.eventId!!

        try {
            localEchoRepository.updateSendState(localId, SendState.SENDING)
            val executeRequest = executeRequest<SendResponse>(eventBus) {
                apiCall = roomAPI.send(
                        localId,
                        roomId = event.roomId ?: "",
                        content = event.content,
                        eventType = event.type
                )
            }
            localEchoRepository.updateSendState(localId, SendState.SENT)
            return executeRequest.eventId
        } catch (e: Throwable) {
            localEchoRepository.updateSendState(localId, SendState.UNDELIVERED)
            throw e
        }
    }

    private suspend fun handleEncryption(params: SendEventTask.Params): Event {
        if (params.cryptoService?.isRoomEncrypted(params.event.roomId ?: "") == true) {
            try {
                return encryptEventTask.execute(EncryptEventTask.Params(
                        params.event.roomId ?: "",
                        params.event,
                        listOf("m.relates_to"),
                        params.cryptoService
                ))
            } catch (throwable: Throwable) {
                // We said it's ok to send verification request in clear
            }
        }
        return params.event
    }
}
