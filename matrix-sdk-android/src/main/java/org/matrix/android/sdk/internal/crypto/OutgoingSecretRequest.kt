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

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.model.OutgoingGossipingRequestState

/**
 * Represents an outgoing room key request
 */
@JsonClass(generateAdapter = true)
internal class OutgoingSecretRequest(
        // Secret Name
        val secretName: String?,
        // list of recipients for the request
        override var recipients: Map<String, List<String>>,
        // Unique id for this request. Used for both
        // an id within the request for later pairing with a cancellation, and for
        // the transaction id when sending the to_device messages to our local
        override var requestId: String,
        // current state of this request
        override var state: OutgoingGossipingRequestState) : OutgoingGossipingRequest {

    // transaction id for the cancellation, if any
}
