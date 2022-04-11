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

package org.matrix.android.sdk.api.session.crypto.keyshare

import org.matrix.android.sdk.api.session.crypto.model.IncomingRequestCancellation
import org.matrix.android.sdk.api.session.crypto.model.IncomingRoomKeyRequest
import org.matrix.android.sdk.api.session.crypto.model.IncomingSecretShareRequest

/**
 * Room keys events listener
 */
interface GossipingRequestListener {
    /**
     * An room key request has been received.
     *
     * @param request the request
     */
    fun onRoomKeyRequest(request: IncomingRoomKeyRequest)

    /**
     * Returns the secret value to be shared
     * @return true if is handled
     */
    fun onSecretShareRequest(request: IncomingSecretShareRequest): Boolean

    /**
     * A room key request cancellation has been received.
     *
     * @param request the cancellation request
     */
    fun onRoomKeyRequestCancellation(request: IncomingRequestCancellation)
}
