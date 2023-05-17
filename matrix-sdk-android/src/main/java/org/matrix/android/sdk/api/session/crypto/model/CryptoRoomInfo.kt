/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

data class CryptoRoomInfo(
        val algorithm: String,
        val shouldEncryptForInvitedMembers: Boolean,
        val blacklistUnverifiedDevices: Boolean,
        // Determines whether or not room history should be shared on new member invites
        val shouldShareHistory: Boolean,
        // This is specific to megolm but not sure how to model it better
        // a security to ensure that a room will never revert to not encrypted
        // even if a new state event with empty encryption, or state is reset somehow
        val wasEncryptedOnce: Boolean,
        // How long the session should be used before changing it. 604800000 (a week) is the recommended default.
        val rotationPeriodMs: Long,
        // How many messages should be sent before changing the session. 100 is the recommended default.
        val rotationPeriodMsgs: Long,
)
