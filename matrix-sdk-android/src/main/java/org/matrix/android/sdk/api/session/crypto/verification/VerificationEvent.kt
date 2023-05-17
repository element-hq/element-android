/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

sealed class VerificationEvent(val transactionId: String, val otherUserId: String) {
    data class RequestAdded(val request: PendingVerificationRequest) : VerificationEvent(request.transactionId, request.otherUserId)
    data class RequestUpdated(val request: PendingVerificationRequest) : VerificationEvent(request.transactionId, request.otherUserId)
    data class TransactionAdded(val transaction: VerificationTransaction) : VerificationEvent(transaction.transactionId, transaction.otherUserId)
    data class TransactionUpdated(val transaction: VerificationTransaction) : VerificationEvent(transaction.transactionId, transaction.otherUserId)
}

fun VerificationEvent.getRequest(): PendingVerificationRequest? {
    return when (this) {
        is VerificationEvent.RequestAdded -> this.request
        is VerificationEvent.RequestUpdated -> this.request
        is VerificationEvent.TransactionAdded -> null
        is VerificationEvent.TransactionUpdated -> null
    }
}

fun VerificationEvent.getTransaction(): VerificationTransaction? {
    return when (this) {
        is VerificationEvent.RequestAdded -> null
        is VerificationEvent.RequestUpdated -> null
        is VerificationEvent.TransactionAdded -> this.transaction
        is VerificationEvent.TransactionUpdated -> this.transaction
    }
}
