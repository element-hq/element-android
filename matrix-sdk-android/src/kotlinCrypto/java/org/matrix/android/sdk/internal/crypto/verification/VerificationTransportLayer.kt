/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.SendToDeviceObject
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.crypto.tasks.SendVerificationMessageTask
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class VerificationTransportLayer @Inject constructor(
        @UserId private val myUserId: String,
        private val sendVerificationMessageTask: SendVerificationMessageTask,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val sendToDeviceTask: SendToDeviceTask,
        private val clock: Clock,
) {

    suspend fun sendToOther(
            request: KotlinVerificationRequest,
            type: String,
            verificationInfo: VerificationInfo<*>,
    ) {
        val roomId = request.roomId
        if (roomId != null) {
            val event = createEventAndLocalEcho(
                    type = type,
                    roomId = roomId,
                    content = verificationInfo.toEventContent()!!
            )
            sendEventInRoom(event)
        } else {
            sendToDeviceEvent(
                    type,
                    verificationInfo.toSendToDeviceObject()!!,
                    request.otherUserId,
                    request.otherDeviceId()?.let { listOf(it) }.orEmpty()
            )
        }
    }

    private fun createEventAndLocalEcho(localId: String = LocalEcho.createLocalEchoId(),
                                        type: String,
                                        roomId: String,
                                        content: Content): Event {
        return Event(
                roomId = roomId,
                originServerTs = clock.epochMillis(),
                senderId = myUserId,
                eventId = localId,
                type = type,
                content = content,
                unsignedData = UnsignedData(age = null, transactionId = localId)
        ).also {
            localEchoEventFactory.createLocalEcho(it)
        }
    }

    suspend fun sendInRoom(type: String,
                           roomId: String,
                           content: Content): String {
        val event = createEventAndLocalEcho(
                type = type,
                roomId = roomId,
                content = content
        )
        return sendEventInRoom(event)
    }

    suspend fun sendEventInRoom(event: Event): String {
        return sendVerificationMessageTask.execute(SendVerificationMessageTask.Params(event, 5)).eventId
    }

    suspend fun sendToDeviceEvent(messageType: String, toSendToDeviceObject: SendToDeviceObject, otherUserId: String, targetDevices: List<String>) {
        // currently to device verification messages are sent unencrypted
        // as per spec not recommended
        // > verification messages may be sent unencrypted, though this is not encouraged.

        val contentMap = MXUsersDevicesMap<Any>()

        targetDevices.forEach {
            contentMap.setObject(otherUserId, it, toSendToDeviceObject)
        }

        sendToDeviceTask
                .execute(SendToDeviceTask.Params(messageType, contentMap))
    }
}
