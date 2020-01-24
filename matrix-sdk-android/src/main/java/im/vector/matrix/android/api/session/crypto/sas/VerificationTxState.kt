/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.crypto.sas

enum class VerificationTxState {
    None,
    // I have started a verification request
    SendingStart,
    Started,
    // Other user/device sent me a request
    OnStarted,
    // I have accepted a request started by the other user/device
    SendingAccept,
    Accepted,
    // My request has been accepted by the other user/device
    OnAccepted,
    // I have sent my public key
    SendingKey,
    KeySent,
    // The other user/device has sent me his public key
    OnKeyReceived,
    // Short code is ready to be displayed
    ShortCodeReady,
    // I have compared the code and manually said that they match
    ShortCodeAccepted,

    SendingMac,
    MacSent,
    Verifying,
    Verified,

    // Global: The verification has been cancelled (by me or other), see cancelReason for details
    Cancelled,
    OnCancelled
}
