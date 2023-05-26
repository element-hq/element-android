/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification

import kotlinx.coroutines.CompletableDeferred
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction

internal sealed class VerificationIntent {
    data class ActionRequestVerification(
            val otherUserId: String,
            // in case of verification in room
            val roomId: String? = null,
            val methods: List<VerificationMethod>,
            // In case of to device it is sent to a list of devices
            val targetDevices: List<String>? = null,
            val deferred: CompletableDeferred<PendingVerificationRequest>,
    ) : VerificationIntent()

    data class OnVerificationRequestReceived(
            val validRequestInfo: ValidVerificationInfoRequest,
            val senderId: String,
            val roomId: String?,
            val timeStamp: Long? = null,
//            val deferred: CompletableDeferred<IVerificationRequest>,
    ) : VerificationIntent()

    data class ActionReadyRequest(
            val transactionId: String,
            val methods: List<VerificationMethod>,
            val deferred: CompletableDeferred<PendingVerificationRequest?>
    ) : VerificationIntent()

    data class OnReadyReceived(
            val transactionId: String,
            val fromUser: String,
            val viaRoom: String?,
            val readyInfo: ValidVerificationInfoReady,
    ) : VerificationIntent()

    data class OnReadyByAnotherOfMySessionReceived(
            val transactionId: String,
            val fromUser: String,
            val viaRoom: String?,
    ) : VerificationIntent()

    data class GetExistingRequestInRoom(
            val transactionId: String,
            val roomId: String,
            val deferred: CompletableDeferred<PendingVerificationRequest?>,
    ) : VerificationIntent()

    data class GetExistingRequest(
            val transactionId: String,
            val otherUserId: String,
            val deferred: CompletableDeferred<PendingVerificationRequest?>,
    ) : VerificationIntent()

    data class GetExistingRequestsForUser(
            val userId: String,
            val deferred: CompletableDeferred<List<PendingVerificationRequest>>,
    ) : VerificationIntent()

    data class GetExistingTransaction(
            val transactionId: String,
            val fromUser: String,
            val deferred: CompletableDeferred<VerificationTransaction?>,
    ) : VerificationIntent()

    data class ActionStartSasVerification(
            val otherUserId: String,
            val requestId: String,
            val deferred: CompletableDeferred<VerificationTransaction>,
    ) : VerificationIntent()

    data class ActionReciprocateQrVerification(
            val otherUserId: String,
            val requestId: String,
            val scannedData: String,
            val deferred: CompletableDeferred<VerificationTransaction?>,
    ) : VerificationIntent()

    data class ActionConfirmCodeWasScanned(
            val otherUserId: String,
            val requestId: String,
            val deferred: CompletableDeferred<Unit>,
    ) : VerificationIntent()

    data class OnStartReceived(
            val viaRoom: String?,
            val fromUser: String,
            val validVerificationInfoStart: ValidVerificationInfoStart,
    ) : VerificationIntent()

    data class OnAcceptReceived(
            val viaRoom: String?,
            val fromUser: String,
            val validAccept: ValidVerificationInfoAccept,
    ) : VerificationIntent()

    data class OnKeyReceived(
            val viaRoom: String?,
            val fromUser: String,
            val validKey: ValidVerificationInfoKey,
    ) : VerificationIntent()

    data class OnMacReceived(
            val viaRoom: String?,
            val fromUser: String,
            val validMac: ValidVerificationInfoMac,
    ) : VerificationIntent()

    data class OnCancelReceived(
            val viaRoom: String?,
            val fromUser: String,
            val validCancel: ValidVerificationInfoCancel,
    ) : VerificationIntent()

    data class ActionSASCodeMatches(
            val transactionId: String,
            val deferred: CompletableDeferred<Unit>
    ) : VerificationIntent()

    data class ActionSASCodeDoesNotMatch(
            val transactionId: String,
            val deferred: CompletableDeferred<Unit>
    ) : VerificationIntent()

    data class ActionCancel(
            val transactionId: String,
            val deferred: CompletableDeferred<Unit>
    ) : VerificationIntent()

    data class OnUnableToDecryptVerificationEvent(
            val transactionId: String,
            val roomId: String,
            val fromUser: String,
    ) : VerificationIntent()

    data class OnDoneReceived(
            val viaRoom: String?,
            val fromUser: String,
            val transactionId: String,
    ) : VerificationIntent()
}
