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

package org.matrix.android.sdk.api.rendezvous

import org.matrix.android.sdk.api.rendezvous.model.ECDHRendezvousCode
import org.matrix.android.sdk.api.rendezvous.model.RendezvousIntent

/**
 * Representation of a rendezvous channel such as that described by MSC3903.
 */
interface RendezvousChannel {
    var transport: RendezvousTransport

    /**
     * @returns the checksum/confirmation digits to be shown to the user
     */
    suspend fun connect(): String

    /**
     * Send a payload via the channel.
     * @param data payload to send
     */
    suspend fun send(data: ByteArray)

    /**
     * Receive a payload from the channel.
     * @returns the received payload
     */
    suspend fun receive(): ByteArray?

    /**
     * @returns a representation of the channel that can be encoded in a QR or similar
     */
    suspend fun close()

    // In future we probably want this to be a more generic RendezvousCode but it is suffice for now
    suspend fun generateCode(intent: RendezvousIntent): ECDHRendezvousCode
    suspend fun cancel(reason: RendezvousFailureReason)
}
