/*
 * Copyright (c) 2020 New Vector Ltd
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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.failure.shouldBeRetried
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.rest.ShareRequestCancellation
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal class CancelGossipRequestWorker(context: Context,
                                         params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val sessionId: String,
            val requestId: String,
            val recipients: Map<String, List<String>>
    ) {
        companion object {
            fun fromRequest(sessionId: String, request: OutgoingGossipingRequest): Params {
                return Params(
                        sessionId = sessionId,
                        requestId = request.requestId,
                        recipients = request.recipients
                )
            }
        }
    }

    @Inject lateinit var sendToDeviceTask: SendToDeviceTask
    @Inject lateinit var cryptoStore: IMXCryptoStore
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var credentials: Credentials

    override suspend fun doWork(): Result {
        val errorOutputData = Data.Builder().putBoolean("failed", true).build()
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success(errorOutputData)

        val sessionComponent = getSessionComponent(params.sessionId)
                ?: return Result.success(errorOutputData).also {
                    // TODO, can this happen? should I update local echo?
                    Timber.e("Unknown Session, cannot send message, sessionId: ${params.sessionId}")
                }
        sessionComponent.inject(this)

        val localId = LocalEcho.createLocalEchoId()
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
                            transactionId = localId
                    )
            )
            cryptoStore.updateOutgoingGossipingRequestState(params.requestId, OutgoingGossipingRequestState.CANCELLED)
            return Result.success()
        } catch (exception: Throwable) {
            return if (exception.shouldBeRetried()) {
                Result.retry()
            } else {
                cryptoStore.updateOutgoingGossipingRequestState(params.requestId, OutgoingGossipingRequestState.FAILED_TO_CANCEL)
                Result.success(errorOutputData)
            }
        }
    }
}
