/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.crypto.model.rest.RoomKeyRequestBody

/**
 * Represents an outgoing room key request
 */
@JsonClass(generateAdapter = true)
data class OutgoingRoomKeyRequest(
        // RequestBody
        var requestBody: RoomKeyRequestBody?,
        // list of recipients for the request
        override var recipients: Map<String, List<String>>,
        // Unique id for this request. Used for both
        // an id within the request for later pairing with a cancellation, and for
        // the transaction id when sending the to_device messages to our local
        override var requestId: String, // current state of this request
        override var state: OutgoingGossipingRequestState
        // transaction id for the cancellation, if any
        // override var cancellationTxnId: String? = null
) : OutgoingGossipingRequest {

    /**
     * Used only for log.
     *
     * @return the room id.
     */
    val roomId: String?
        get() = if (null != requestBody) {
            requestBody!!.roomId
        } else null

    /**
     * Used only for log.
     *
     * @return the session id
     */
    val sessionId: String?
        get() = if (null != requestBody) {
            requestBody!!.sessionId
        } else null
}
