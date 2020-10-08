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
package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.internal.session.room.send.LocalEchoRepository
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitCallback
import javax.inject.Inject

internal interface EncryptEventTask : Task<EncryptEventTask.Params, Event> {
    data class Params(val roomId: String,
                      val event: Event,
                      /**Do not encrypt these keys, keep them as is in encrypted content (e.g. m.relates_to)*/
                      val keepKeys: List<String>? = null,
                      val crypto: CryptoService
    )
}

internal class DefaultEncryptEventTask @Inject constructor(
//        private val crypto: CryptoService
        private val localEchoRepository: LocalEchoRepository
) : EncryptEventTask {
    override suspend fun execute(params: EncryptEventTask.Params): Event {
        if (!params.crypto.isRoomEncrypted(params.roomId)) return params.event
        val localEvent = params.event
        if (localEvent.eventId == null) {
            throw IllegalArgumentException()
        }

        localEchoRepository.updateSendState(localEvent.eventId, SendState.ENCRYPTING)

        val localMutableContent = localEvent.content?.toMutableMap() ?: mutableMapOf()
        params.keepKeys?.forEach {
            localMutableContent.remove(it)
        }

//        try {
        awaitCallback<MXEncryptEventContentResult> {
            params.crypto.encryptEventContent(localMutableContent, localEvent.type, params.roomId, it)
        }.let { result ->
            val modifiedContent = HashMap(result.eventContent)
            params.keepKeys?.forEach { toKeep ->
                localEvent.content?.get(toKeep)?.let {
                    // put it back in the encrypted thing
                    modifiedContent[toKeep] = it
                }
            }
            val safeResult = result.copy(eventContent = modifiedContent)
            return localEvent.copy(
                    type = safeResult.eventType,
                    content = safeResult.eventContent
            )
        }
//        } catch (throwable: Throwable) {
//            val sendState = when (throwable) {
//                is Failure.CryptoError -> SendState.FAILED_UNKNOWN_DEVICES
//                else                   -> SendState.UNDELIVERED
//            }
//            localEchoUpdater.updateSendState(localEvent.eventId, sendState)
//            throw throwable
//        }
    }
}
