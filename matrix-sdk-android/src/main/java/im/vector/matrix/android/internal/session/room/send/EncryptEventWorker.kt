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

package im.vector.matrix.android.internal.session.room.send

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.crypto.model.MXEncryptEventContentResult
import im.vector.matrix.android.internal.util.awaitCallback
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import timber.log.Timber
import javax.inject.Inject

internal class EncryptEventWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val roomId: String,
            val event: Event,
            /**Do not encrypt these keys, keep them as is in encrypted content (e.g. m.relates_to)*/
            val keepKeys: List<String>? = null,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var crypto: CryptoService
    @Inject lateinit var localEchoUpdater: LocalEchoUpdater

    override suspend fun doWork(): Result {
        Timber.v("Start Encrypt work")
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success().also {
                    Timber.v("Work cancelled due to input error from parent")
                }

        Timber.v("Start Encrypt work for event ${params.event.eventId}")
        if (params.lastFailureMessage != null) {
            // Transmit the error
            return Result.success(inputData)
        }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        val localEvent = params.event
        if (localEvent.eventId == null) {
            return Result.success()
        }
        localEchoUpdater.updateSendState(localEvent.eventId, SendState.ENCRYPTING)

        val localMutableContent = localEvent.content?.toMutableMap() ?: mutableMapOf()
        params.keepKeys?.forEach {
            localMutableContent.remove(it)
        }

        var error: Throwable? = null
        var result: MXEncryptEventContentResult? = null
        try {
            result = awaitCallback {
                crypto.encryptEventContent(localMutableContent, localEvent.type, params.roomId, it)
            }
        } catch (throwable: Throwable) {
            error = throwable
        }
        if (result != null) {
            val modifiedContent = HashMap(result.eventContent)
            params.keepKeys?.forEach { toKeep ->
                localEvent.content?.get(toKeep)?.let {
                    // put it back in the encrypted thing
                    modifiedContent[toKeep] = it
                }
            }
            val safeResult = result.copy(eventContent = modifiedContent)
            val encryptedEvent = localEvent.copy(
                    type = safeResult.eventType,
                    content = safeResult.eventContent
            )
            val nextWorkerParams = SendEventWorker.Params(params.sessionId, params.roomId, encryptedEvent)
            return Result.success(WorkerParamsFactory.toData(nextWorkerParams))
        } else {
            val sendState = when (error) {
                is Failure.CryptoError -> SendState.FAILED_UNKNOWN_DEVICES
                else                   -> SendState.UNDELIVERED
            }
            localEchoUpdater.updateSendState(localEvent.eventId, sendState)
            // always return success, or the chain will be stuck for ever!
            val nextWorkerParams = SendEventWorker.Params(params.sessionId, params.roomId, localEvent, error?.localizedMessage
                    ?: "Error")
            return Result.success(WorkerParamsFactory.toData(nextWorkerParams))
        }
    }
}
