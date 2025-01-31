/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.model

import org.matrix.android.sdk.internal.util.time.Clock

/**
 * IncomingRoomKeyRequest class defines the incoming room keys request.
 */
data class IncomingRoomKeyRequest(
        /**
         * The user id.
         */
        val userId: String? = null,

        /**
         * The device id.
         */
        val deviceId: String? = null,

        /**
         * The request id.
         */
        val requestId: String? = null,

        /**
         * The request body.
         */
        val requestBody: RoomKeyRequestBody? = null,

        val localCreationTimestamp: Long?
) {
    companion object {
        /**
         * Factory.
         *
         * @param trail the AuditTrail data
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

        internal fun fromRestRequest(senderId: String, request: RoomKeyShareRequest, clock: Clock): IncomingRoomKeyRequest? {
            return IncomingRoomKeyRequest(
                    userId = senderId,
                    deviceId = request.requestingDeviceId,
                    requestId = request.requestId,
                    requestBody = request.body,
                    localCreationTimestamp = clock.epochMillis()
            )
        }
    }
}
