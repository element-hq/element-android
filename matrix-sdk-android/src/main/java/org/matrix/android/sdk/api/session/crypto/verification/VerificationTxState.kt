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

sealed class VerificationTxState {
    // Uninitialized state
    object None : VerificationTxState()

    // Specific for SAS
    abstract class VerificationSasTxState : VerificationTxState()

    object SendingStart : VerificationSasTxState()
    object Started : VerificationSasTxState()
    object OnStarted : VerificationSasTxState()
    object SendingAccept : VerificationSasTxState()
    object Accepted : VerificationSasTxState()
    object OnAccepted : VerificationSasTxState()
    object SendingKey : VerificationSasTxState()
    object KeySent : VerificationSasTxState()
    object OnKeyReceived : VerificationSasTxState()
    object ShortCodeReady : VerificationSasTxState()
    object ShortCodeAccepted : VerificationSasTxState()
    object SendingMac : VerificationSasTxState()
    object MacSent : VerificationSasTxState()
    object Verifying : VerificationSasTxState()

    // Specific for QR code
    abstract class VerificationQrTxState : VerificationTxState()

    // Will be used to ask the user if the other user has correctly scanned
    object QrScannedByOther : VerificationQrTxState()
    object WaitingOtherReciprocateConfirm : VerificationQrTxState()

    // Terminal states
    abstract class TerminalTxState : VerificationTxState()

    object Verified : TerminalTxState()

    // Cancelled by me or by other
    data class Cancelled(val cancelCode: CancelCode, val byMe: Boolean) : TerminalTxState()
}
