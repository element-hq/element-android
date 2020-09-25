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
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.greenrobot.eventbus.EventBus
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.failure.shouldBeRetried
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import org.matrix.android.sdk.internal.crypto.actions.MessageEncrypter
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.event.SecretSendEventContent
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import timber.log.Timber
import javax.inject.Inject

internal class SendGossipWorker(context: Context,
                                params: WorkerParameters)
    : SessionSafeCoroutineWorker<SendGossipWorker.Params>(context, params, Params::class.java) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val secretValue: String,
            val request: IncomingSecretShareRequest,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var sendToDeviceTask: SendToDeviceTask
    @Inject lateinit var cryptoStore: IMXCryptoStore
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var credentials: Credentials
    @Inject lateinit var messageEncrypter: MessageEncrypter
    @Inject lateinit var ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: Params): Result {
        val localId = LocalEcho.createLocalEchoId()
        val eventType: String = EventType.SEND_SECRET

        val toDeviceContent = SecretSendEventContent(
                requestId = params.request.requestId ?: "",
                secretValue = params.secretValue
        )

        val requestingUserId = params.request.userId ?: ""
        val requestingDeviceId = params.request.deviceId ?: ""
        val deviceInfo = cryptoStore.getUserDevice(requestingUserId, requestingDeviceId)
                ?: return buildErrorResult(params, "Unknown deviceInfo, cannot send message").also {
                    cryptoStore.updateGossipingRequestState(params.request, GossipingRequestState.FAILED_TO_ACCEPTED)
                    Timber.e("Unknown deviceInfo, cannot send message, sessionId: ${params.request.deviceId}")
                }

        val sendToDeviceMap = MXUsersDevicesMap<Any>()

        val devicesByUser = mapOf(requestingUserId to listOf(deviceInfo))
        val usersDeviceMap = ensureOlmSessionsForDevicesAction.handle(devicesByUser)
        val olmSessionResult = usersDeviceMap.getObject(requestingUserId, requestingDeviceId)
        if (olmSessionResult?.sessionId == null) {
            // no session with this device, probably because there
            // were no one-time keys.
            return buildErrorResult(params, "no session with this device").also {
                cryptoStore.updateGossipingRequestState(params.request, GossipingRequestState.FAILED_TO_ACCEPTED)
                Timber.e("no session with this device, probably because there were no one-time keys.")
            }
        }

        val payloadJson = mapOf(
                "type" to EventType.SEND_SECRET,
                "content" to toDeviceContent.toContent()
        )

        try {
            val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo))
            sendToDeviceMap.setObject(requestingUserId, requestingDeviceId, encodedPayload)
        } catch (failure: Throwable) {
            Timber.e("## Fail to encrypt gossip + ${failure.localizedMessage}")
        }

        cryptoStore.saveGossipingEvent(Event(
                type = eventType,
                content = toDeviceContent.toContent(),
                senderId = credentials.userId
        ).also {
            it.ageLocalTs = System.currentTimeMillis()
        })

        try {
            sendToDeviceTask.execute(
                    SendToDeviceTask.Params(
                            eventType = EventType.ENCRYPTED,
                            contentMap = sendToDeviceMap,
                            transactionId = localId
                    )
            )
            cryptoStore.updateGossipingRequestState(params.request, GossipingRequestState.ACCEPTED)
            return Result.success()
        } catch (throwable: Throwable) {
            return if (throwable.shouldBeRetried()) {
                Result.retry()
            } else {
                cryptoStore.updateGossipingRequestState(params.request, GossipingRequestState.FAILED_TO_ACCEPTED)
                buildErrorResult(params, throwable.localizedMessage ?: "error")
            }
        }
    }

    override fun buildErrorParams(params: Params, message: String): Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }
}
