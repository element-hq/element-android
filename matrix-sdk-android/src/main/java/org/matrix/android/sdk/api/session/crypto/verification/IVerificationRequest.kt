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

package org.matrix.android.sdk.api.session.crypto.verification

import org.matrix.android.sdk.internal.crypto.verification.KotlinVerificationRequest

enum class EVerificationState {
    // outgoing started request
    WaitingForReady,
    // for incoming
    Requested,
    // both incoming/outgoing
    Ready,
    Started,
    WeStarted,
    WaitingForDone,
    Done,
    Cancelled,
    HandledByOtherSession
}

// TODO remove that
interface IVerificationRequest{

    fun requestId(): String

    fun incoming(): Boolean
    fun otherUserId(): String
    fun roomId(): String?
    // target devices in case of to_device self verification
    fun targetDevices() : List<String>?

    fun state(): EVerificationState
    fun ageLocalTs(): Long

    fun isSasSupported(): Boolean
    fun otherCanShowQrCode(): Boolean
    fun otherCanScanQrCode(): Boolean

    fun otherDeviceId(): String?

    fun qrCodeText() : String?

    fun isFinished() : Boolean = state() == EVerificationState.Cancelled || state() == EVerificationState.Done

    fun cancelCode(): CancelCode?

}
