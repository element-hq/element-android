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

package im.vector.riotx.features.crypto.verification

import im.vector.riotx.core.platform.VectorViewModelAction

sealed class VerificationAction : VectorViewModelAction {
    data class RequestVerificationByDM(val userID: String, val roomId: String?) : VerificationAction()
    data class StartSASVerification(val userID: String, val pendingRequestTransactionId: String) : VerificationAction()
    data class RemoteQrCodeScanned(val userID: String, val sasTransactionId: String, val scannedData: String) : VerificationAction()
    data class SASMatchAction(val userID: String, val sasTransactionId: String) : VerificationAction()
    data class SASDoNotMatchAction(val userID: String, val sasTransactionId: String) : VerificationAction()
    object GotItConclusion : VerificationAction()
}
