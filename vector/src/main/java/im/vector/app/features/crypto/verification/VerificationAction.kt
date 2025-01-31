/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification

import im.vector.app.core.platform.VectorViewModelAction

// TODO Remove otherUserId and transactionId when it's not necessary. Should be known by the ViewModel, no?
sealed class VerificationAction : VectorViewModelAction {
    data class RequestVerificationByDM(val otherUserId: String, val roomId: String?) : VerificationAction()
    data class StartSASVerification(val otherUserId: String, val pendingRequestTransactionId: String) : VerificationAction()
    data class RemoteQrCodeScanned(val otherUserId: String, val transactionId: String, val scannedData: String) : VerificationAction()
    object OtherUserScannedSuccessfully : VerificationAction()
    object OtherUserDidNotScanned : VerificationAction()
    data class SASMatchAction(val otherUserId: String, val sasTransactionId: String) : VerificationAction()
    data class SASDoNotMatchAction(val otherUserId: String, val sasTransactionId: String) : VerificationAction()
    data class GotItConclusion(val verified: Boolean) : VerificationAction()
    object SkipVerification : VerificationAction()
    object VerifyFromPassphrase : VerificationAction()
    object ReadyPendingVerification : VerificationAction()
    object CancelPendingVerification : VerificationAction()
    data class GotResultFromSsss(val cypherData: String, val alias: String) : VerificationAction()
    object CancelledFromSsss : VerificationAction()
    object SecuredStorageHasBeenReset : VerificationAction()
}
