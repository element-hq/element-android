/*
 * Copyright 2020 New Vector Ltd
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
    object GotItConclusion : VerificationAction()
    object SkipVerification : VerificationAction()
    object VerifyFromPassphrase : VerificationAction()
    data class GotResultFromSsss(val cypherData: String, val alias: String) : VerificationAction()
    object CancelledFromSsss : VerificationAction()
    object SecuredStorageHasBeenReset : VerificationAction()
}
