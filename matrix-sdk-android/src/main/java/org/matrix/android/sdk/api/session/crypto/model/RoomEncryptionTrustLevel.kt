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

/**
 * RoomEncryptionTrustLevel represents the trust level in an encrypted room.
 */
enum class RoomEncryptionTrustLevel {
    // No one in the room has been verified -> Black shield
    Default,

    // There are one or more device un-verified -> the app should display a red shield
    Warning,

    // All devices in the room are verified -> the app should display a green shield
    Trusted,

    // e2e is active but with an unsupported algorithm
    E2EWithUnsupportedAlgorithm
}
