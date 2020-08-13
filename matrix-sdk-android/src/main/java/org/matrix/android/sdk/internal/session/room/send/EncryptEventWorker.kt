/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room.send

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.MXEventDecryptionResult
import org.matrix.android.sdk.internal.crypto.model.MXEncryptEventContentResult
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import timber.log.Timber
import javax.inject.Inject

/**
 * Possible previous worker: None
 * Possible next worker    : Always [SendEventWorker]
 */
internal class EncryptEventWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val event: Event,
            /** Do not encrypt these keys, keep them as is in encrypted content (e.g. m.relates_to) */
            val keepKeys: List<String>? = null,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var crypto: CryptoService
    @Inject lateinit var localEchoRepository: LocalEchoRepository

    override suspend fun doWork(): Result {
        Timber.v("Start Encrypt work")
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()
                        .also { Timber.e("Unable to parse work parameters") }

        Timber.v("Start Encrypt work for event ${params.event.eventId}")
        if (params.lastFailureMessage != null) {
            // Transmit the error
            return Result.success(inputData)
                    .also { Timber.e("Work cancelled due to input error from parent") }
        }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        val localEvent = params.event
        if (localEvent.eventId == null) {
            return Result.success()
        }
        localEchoRepository.updateSendState(localEvent.eventId, SendState.ENCRYPTING)

        val localMutableContent = localEvent.content?.toMutableMap() ?: mutableMapOf()
        params.keepKeys?.forEach {
            localMutableContent.remove(it)
        }

        var error: Throwable? = null
        var result: MXEncryptEventContentResult? = null
        try {
            result = awaitCallback {
                crypto.encryptEventContent(localMutableContent, localEvent.type, localEvent.roomId!!, it)
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
            // Better handling of local echo, to avoid decrypting transition on remote echo
            // Should I only do it for text messages?
            if (result.eventContent["algorithm"] == MXCRYPTO_ALGORITHM_MEGOLM) {
                val decryptionLocalEcho = MXEventDecryptionResult(
                        clearEvent = Event(
                                type = localEvent.type,
                                content = localEvent.content,
                                roomId = localEvent.roomId
                        ).toContent(),
                        forwardingCurve25519KeyChain = emptyList(),
                        senderCurve25519Key = result.eventContent["sender_key"] as? String,
                        claimedEd25519Key = crypto.getMyDevice().fingerprint()
                )
                localEchoRepository.updateEncryptedEcho(localEvent.eventId, safeResult.eventContent, decryptionLocalEcho)
            }

            val nextWorkerParams = SendEventWorker.Params(params.sessionId, encryptedEvent)
            return Result.success(WorkerParamsFactory.toData(nextWorkerParams))
        } else {
            val sendState = when (error) {
                is Failure.CryptoError -> SendState.FAILED_UNKNOWN_DEVICES
                else                   -> SendState.UNDELIVERED
            }
            localEchoRepository.updateSendState(localEvent.eventId, sendState)
            // always return success, or the chain will be stuck for ever!
            val nextWorkerParams = SendEventWorker.Params(params.sessionId, localEvent, error?.localizedMessage
                    ?: "Error")
            return Result.success(WorkerParamsFactory.toData(nextWorkerParams))
        }
    }
}
