/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.session.crypto.model.RoomKeyRequestBody
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode

data class RequestReply(
        val userId: String,
        val fromDevice: String?,
        val result: RequestResult
)

sealed class RequestResult {
    data class  Success(val chainIndex: Int) : RequestResult()
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
