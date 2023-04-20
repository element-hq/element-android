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

package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import javax.inject.Inject

internal class VerificationRequestsStore @Inject constructor() {

    // map [sender : [transaction]]
    private val txMap = HashMap<String, MutableMap<String, VerificationTransaction>>()

    // we need to keep track of finished transaction
    // It will be used for gossiping (to send request after request is completed and 'done' by other)
    private val pastTransactions = HashMap<String, MutableMap<String, VerificationTransaction>>()

    /**
     * Map [sender: [PendingVerificationRequest]]
     * For now we keep all requests (even terminated ones) during the lifetime of the app.
     */
    private val pendingRequests = HashMap<String, MutableList<KotlinVerificationRequest>>()

    fun getExistingRequest(fromUser: String, requestId: String): KotlinVerificationRequest? {
        return pendingRequests[fromUser]?.firstOrNull { it.requestId == requestId }
    }

    fun getExistingRequestsForUser(fromUser: String): List<KotlinVerificationRequest> {
        return pendingRequests[fromUser].orEmpty()
    }

    fun getExistingRequestInRoom(requestId: String, roomId: String): KotlinVerificationRequest? {
        return pendingRequests.flatMap { entry ->
            entry.value.filter { it.roomId == roomId && it.requestId == requestId }
        }.firstOrNull()
    }

    fun getExistingRequestWithRequestId(requestId: String): KotlinVerificationRequest? {
        return pendingRequests
                .flatMap { it.value }
                .firstOrNull { it.requestId == requestId }
    }

    fun getExistingTransaction(fromUser: String, transactionId: String): VerificationTransaction? {
        return txMap[fromUser]?.get(transactionId)
    }

    fun getExistingTransaction(transactionId: String): VerificationTransaction? {
        txMap.forEach {
            val match = it.value.values
                    .firstOrNull { it.transactionId == transactionId }
            if (match != null) return match
        }
        return null
    }

    fun deleteTransaction(fromUser: String, transactionId: String) {
        txMap[fromUser]?.remove(transactionId)
    }

    fun deleteRequest(request: PendingVerificationRequest) {
        val requestsForUser = pendingRequests.getOrPut(request.otherUserId) { mutableListOf() }
        val index = requestsForUser.indexOfFirst {
            it.requestId == request.transactionId
        }
        if (index != -1) {
            requestsForUser.removeAt(index)
        }
    }

//    fun deleteRequest(otherUserId: String, transactionId: String) {
//        txMap[otherUserId]?.remove(transactionId)
//    }

    fun addRequest(otherUserId: String, request: KotlinVerificationRequest) {
        pendingRequests.getOrPut(otherUserId) { mutableListOf() }
                .add(request)
    }

    fun addTransaction(transaction: VerificationTransaction) {
        val txInnerMap = txMap.getOrPut(transaction.otherUserId) { mutableMapOf() }
        txInnerMap[transaction.transactionId] = transaction
    }

    fun rememberPastSuccessfulTransaction(transaction: VerificationTransaction) {
        val transactionId = transaction.transactionId
        pastTransactions.getOrPut(transactionId) { mutableMapOf() }[transactionId] = transaction
    }
}
