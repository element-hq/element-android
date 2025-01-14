/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification

import im.vector.app.core.platform.VectorViewModelAction

sealed class VerificationAction : VectorViewModelAction {
    object RequestVerificationByDM : VerificationAction()
    object RequestSelfVerification : VerificationAction()
    object StartSASVerification : VerificationAction()
    data class RemoteQrCodeScanned(val otherUserId: String, val transactionId: String, val scannedData: String) : VerificationAction()
    object OtherUserScannedSuccessfully : VerificationAction()
    object OtherUserDidNotScanned : VerificationAction()
    object SASMatchAction : VerificationAction()
    object SASDoNotMatchAction : VerificationAction()
    data class GotItConclusion(val verified: Boolean) : VerificationAction()
    object FailedToGetKeysFrom4S : VerificationAction()
    object SkipVerification : VerificationAction()
    object ForgotResetAll : VerificationAction()
    object VerifyFromPassphrase : VerificationAction()
    object ReadyPendingVerification : VerificationAction()
    object CancelPendingVerification : VerificationAction()
    data class GotResultFromSsss(val cypherData: String, val alias: String) : VerificationAction()
    object CancelledFromSsss : VerificationAction()
    object SecuredStorageHasBeenReset : VerificationAction()
    object SelfVerificationWasNotMe : VerificationAction()
}
