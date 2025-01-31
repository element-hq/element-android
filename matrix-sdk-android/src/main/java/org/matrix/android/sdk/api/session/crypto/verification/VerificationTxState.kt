/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.verification

sealed class VerificationTxState {
    /**
     * Uninitialized state.
     */
    object None : VerificationTxState()

    /**
     * Specific for SAS.
     */
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

    /**
     * Specific for QR code.
     */
    abstract class VerificationQrTxState : VerificationTxState()

    /**
     * Will be used to ask the user if the other user has correctly scanned.
     */
    object QrScannedByOther : VerificationQrTxState()
    object WaitingOtherReciprocateConfirm : VerificationQrTxState()

    /**
     * Terminal states.
     */
    abstract class TerminalTxState : VerificationTxState()

    object Verified : TerminalTxState()

    /**
     * Cancelled by me or by other.
     */
    data class Cancelled(val cancelCode: CancelCode, val byMe: Boolean) : TerminalTxState()
}
