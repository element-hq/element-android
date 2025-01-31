/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto

import org.matrix.android.sdk.api.session.crypto.model.RoomKeyRequestBody
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode

data class RequestReply(
        val userId: String,
        val fromDevice: String?,
        val result: RequestResult
)

sealed class RequestResult {
    data class Success(val chainIndex: Int) : RequestResult()
    data class Failure(val code: WithHeldCode) : RequestResult()
}

data class OutgoingKeyRequest(
        var requestBody: RoomKeyRequestBody?,
        // recipients for the request map of users to list of deviceId
        val recipients: Map<String, List<String>>,
        val fromIndex: Int,
        // Unique id for this request. Used for both
        // an id within the request for later pairing with a cancellation, and for
        // the transaction id when sending the to_device messages to our local
        val requestId: String, // current state of this request
        val state: OutgoingRoomKeyRequestState,
        val results: List<RequestReply>
) {
    /**
     * Used only for log.
     *
     * @return the room id.
     */
    val roomId = requestBody?.roomId

    /**
     * Used only for log.
     *
     * @return the session id
     */
    val sessionId = requestBody?.sessionId
}
