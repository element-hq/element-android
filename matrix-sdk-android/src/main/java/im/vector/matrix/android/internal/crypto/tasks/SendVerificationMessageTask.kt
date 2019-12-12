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
package im.vector.matrix.android.internal.crypto.tasks

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.room.send.LocalEchoUpdater
import im.vector.matrix.android.internal.session.room.send.SendResponse
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal interface SendVerificationMessageTask : Task<SendVerificationMessageTask.Params, SendResponse> {
    data class Params(
            val type: String,
            val event: Event,
            val cryptoService: CryptoService?
    )

    fun createParamsAndLocalEcho(type: String,
                                 roomId: String,
                                 content: Content,
                                 cryptoService: CryptoService?) : Params
}

internal class DefaultSendVerificationMessageTask @Inject constructor(
        private val localEchoUpdater: LocalEchoUpdater,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val encryptEventTask: DefaultEncryptEventTask,
        private val monarchy: Monarchy,
        @UserId private val userId: String,
        private val roomAPI: RoomAPI) : SendVerificationMessageTask {

    override fun createParamsAndLocalEcho(type: String, roomId: String, content: Content, cryptoService: CryptoService?): SendVerificationMessageTask.Params {
        val localID = LocalEcho.createLocalEchoId()
        val event = Event(
                roomId = roomId,
                originServerTs = System.currentTimeMillis(),
                senderId = userId,
                eventId = localID,
                type = type,
                content = content,
                unsignedData = UnsignedData(age = null, transactionId = localID)
        ).also {
            localEchoEventFactory.saveLocalEcho(monarchy, it)
        }
        return SendVerificationMessageTask.Params(
                type,
                event,
                cryptoService
        )
    }

    override suspend fun execute(params: SendVerificationMessageTask.Params): SendResponse {
        val event = handleEncryption(params)
        val localID = event.eventId!!

        try {
            localEchoUpdater.updateSendState(localID, SendState.SENDING)
            val executeRequest = executeRequest<SendResponse> {
                apiCall = roomAPI.send(
                        localID,
                        roomId = event.roomId ?: "",
                        content = event.content,
                        eventType = event.type
                )
            }
            localEchoUpdater.updateSendState(localID, SendState.SENT)
            return executeRequest
        } catch (e: Throwable) {
            localEchoUpdater.updateSendState(localID, SendState.UNDELIVERED)
            throw e
        }
    }

    private suspend fun handleEncryption(params: SendVerificationMessageTask.Params): Event {
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
