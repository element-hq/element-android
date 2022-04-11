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

package org.matrix.android.sdk.internal.crypto

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.failure.shouldBeRetried
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.OutgoingGossipingRequestState
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.crypto.model.rest.ShareRequestCancellation
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.createUniqueTxnId
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import javax.inject.Inject

internal class CancelGossipRequestWorker(context: Context, params: WorkerParameters, sessionManager: SessionManager) :
    SessionSafeCoroutineWorker<CancelGossipRequestWorker.Params>(context, params, sessionManager, Params::class.java) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val requestId: String,
            val recipients: Map<String, List<String>>,
            // The txnId for the sendToDevice request. Nullable for compatibility reasons, but MUST always be provided
            // to use the same value if this worker is retried.
            val txnId: String? = null,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams {
        companion object {
            fun fromRequest(sessionId: String, request: OutgoingGossipingRequest): Params {
                return Params(
                        sessionId = sessionId,
                        requestId = request.requestId,
                        recipients = request.recipients,
                        txnId = createUniqueTxnId(),
                        lastFailureMessage = null
                )
            }
        }
    }

    @Inject lateinit var sendToDeviceTask: SendToDeviceTask
    @Inject lateinit var cryptoStore: IMXCryptoStore
    @Inject lateinit var credentials: Credentials

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: Params): Result {
        // params.txnId should be provided in all cases now. But Params can be deserialized by
        // the WorkManager from data serialized in a previous version of the application, so without the txnId field.
        // So if not present, we create a txnId
        val txnId = params.txnId ?: createUniqueTxnId()
        val contentMap = MXUsersDevicesMap<Any>()
        val toDeviceContent = ShareRequestCancellation(
                requestingDeviceId = credentials.deviceId,
                requestId = params.requestId
        )
        cryptoStore.saveGossipingEvent(Event(
                type = EventType.ROOM_KEY_REQUEST,
                content = toDeviceContent.toContent(),
                senderId = credentials.userId
        ).also {
            it.ageLocalTs = System.currentTimeMillis()
        })

        params.recipients.forEach { userToDeviceMap ->
            userToDeviceMap.value.forEach { deviceId ->
                contentMap.setObject(userToDeviceMap.key, deviceId, toDeviceContent)
            }
        }

        try {
            cryptoStore.updateOutgoingGossipingRequestState(params.requestId, OutgoingGossipingRequestState.CANCELLING)
            sendToDeviceTask.execute(
                    SendToDeviceTask.Params(
                            eventType = EventType.ROOM_KEY_REQUEST,
                            contentMap = contentMap,
                            transactionId = txnId
                    )
            )
            cryptoStore.updateOutgoingGossipingRequestState(params.requestId, OutgoingGossipingRequestState.CANCELLED)
            return Result.success()
        } catch (throwable: Throwable) {
            return if (throwable.shouldBeRetried()) {
                Result.retry()
            } else {
                cryptoStore.updateOutgoingGossipingRequestState(params.requestId, OutgoingGossipingRequestState.FAILED_TO_CANCEL)
                buildErrorResult(params, throwable.localizedMessage ?: "error")
            }
        }
    }

    override fun buildErrorParams(params: Params, message: String): Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }
}
