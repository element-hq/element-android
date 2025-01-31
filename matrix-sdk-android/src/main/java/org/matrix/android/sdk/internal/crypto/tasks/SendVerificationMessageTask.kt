/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.CryptoSessionInfoProvider
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.toMatrixErrorStr
import javax.inject.Inject

internal interface SendVerificationMessageTask : Task<SendVerificationMessageTask.Params, String> {
    data class Params(
            val event: Event
    )
}

internal class DefaultSendVerificationMessageTask @Inject constructor(
        private val localEchoRepository: LocalEchoRepository,
        private val encryptEventTask: EncryptEventTask,
        private val roomAPI: RoomAPI,
        private val cryptoSessionInfoProvider: CryptoSessionInfoProvider,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SendVerificationMessageTask {

    override suspend fun execute(params: SendVerificationMessageTask.Params): String {
        val event = handleEncryption(params)
        val localId = event.eventId!!

        try {
            localEchoRepository.updateSendState(localId, event.roomId, SendState.SENDING)
            val response = executeRequest(globalErrorReceiver) {
                roomAPI.send(
                        txId = localId,
                        roomId = event.roomId ?: "",
                        content = event.content,
                        eventType = event.type ?: ""
                )
            }
            localEchoRepository.updateSendState(localId, event.roomId, SendState.SENT)
            return response.eventId
        } catch (e: Throwable) {
            localEchoRepository.updateSendState(localId, event.roomId, SendState.UNDELIVERED, e.toMatrixErrorStr())
            throw e
        }
    }

    private suspend fun handleEncryption(params: SendVerificationMessageTask.Params): Event {
        if (cryptoSessionInfoProvider.isRoomEncrypted(params.event.roomId ?: "")) {
            try {
                return encryptEventTask.execute(
                        EncryptEventTask.Params(
                                params.event.roomId ?: "",
                                params.event,
                                listOf("m.relates_to")
                        )
                )
            } catch (throwable: Throwable) {
                // We said it's ok to send verification request in clear
            }
        }
        return params.event
    }
}
