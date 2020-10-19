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

package org.matrix.android.sdk.api.crypto

/**
 * Class to define the parameters used to customize or configure the end-to-end crypto.
 */
data class MXCryptoConfig constructor(
        // Tell whether the encryption of the event content is enabled for the invited members.
        // SDK clients can disable this by settings it to false.
        // Note that the encryption for the invited members will be blocked if the history visibility is "joined".
        val enableEncryptionForInvitedMembers: Boolean = true,

        /**
         * If set to true, the SDK will automatically ignore room key request (gossiping)
         * coming from your other untrusted sessions (or blocked).
         * If set to false, the request will be forwarded to the application layer; in this
         * case the application can decide to prompt the user.
         */
        val discardRoomKeyRequestsFromUntrustedDevices: Boolean = true
)
