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

package org.matrix.android.sdk.api.session.crypto.model

import org.matrix.android.sdk.internal.crypto.model.AuditTrail
import org.matrix.android.sdk.internal.crypto.model.IncomingKeyRequestInfo
import org.matrix.android.sdk.internal.crypto.model.TrailType

/**
 * IncomingRoomKeyRequest class defines the incoming room keys request.
 */
data class IncomingRoomKeyRequest(
        /**
         * The user id
         */
        val userId: String? = null,

        /**
         * The device id
         */
        val deviceId: String? = null,

        /**
         * The request id
         */
        val requestId: String? = null,

        /**
         * The request body
         */
        val requestBody: RoomKeyRequestBody? = null,

        val localCreationTimestamp: Long?
) {
    companion object {
        /**
         * Factory
         *
         * @param event the event
         */
        fun fromEvent(trail: AuditTrail): IncomingRoomKeyRequest? {
            return trail
                    .takeIf { it.type == TrailType.IncomingKeyRequest }
                    ?.let {
                        it.info as? IncomingKeyRequestInfo
                    }
                    ?.let {
                        IncomingRoomKeyRequest(
                                userId = it.userId,
                                deviceId = it.deviceId,
                                requestId = it.requestId,
                                requestBody = RoomKeyRequestBody(
                                        algorithm = it.alg,
                                        roomId = it.roomId,
                                        senderKey = it.senderKey,
                                        sessionId = it.sessionId
                                ),
                                localCreationTimestamp = trail.ageLocalTs
                        )
                    }
        }

        fun fromRestRequest(senderId: String, request: RoomKeyShareRequest): IncomingRoomKeyRequest? {
            return IncomingRoomKeyRequest(
                    userId = senderId,
                    deviceId = request.requestingDeviceId,
                    requestId = request.requestId,
                    requestBody = request.body,
                    localCreationTimestamp = System.currentTimeMillis()
            )
        }
    }
}
