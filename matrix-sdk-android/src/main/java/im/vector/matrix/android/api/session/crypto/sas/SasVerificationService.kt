/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.crypto.sas

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest

/**
 * https://matrix.org/docs/spec/client_server/r0.5.0#key-verification-framework
 *
 * Verifying keys manually by reading out the Ed25519 key is not very user friendly, and can lead to errors.
 * SAS verification is a user-friendly key verification process.
 * SAS verification is intended to be a highly interactive process for users,
 * and as such exposes verification methods which are easier for users to use.
 */
interface SasVerificationService {

    fun addListener(listener: SasVerificationListener)

    fun removeListener(listener: SasVerificationListener)

    /**
     * Mark this device as verified manually
     */
    fun markedLocallyAsManuallyVerified(userId: String, deviceID: String)

    fun getExistingTransaction(otherUser: String, tid: String): SasVerificationTransaction?

    fun getExistingVerificationRequest(otherUser: String): List<PendingVerificationRequest>?

    fun getExistingVerificationRequest(otherUser: String, tid: String?): PendingVerificationRequest?

    /**
     * Shortcut for KeyVerificationStart.VERIF_METHOD_SAS
     * @see beginKeyVerification
     */
    fun beginKeyVerificationSAS(userId: String, deviceID: String): String?

    /**
     * Request a key verification from another user using toDevice events.
     */
    fun beginKeyVerification(method: String, userId: String, deviceID: String): String?

    fun requestKeyVerificationInDMs(userId: String, roomId: String): PendingVerificationRequest

    fun declineVerificationRequestInDMs(otherUserId: String, otherDeviceId: String, transactionId: String, roomId: String)

    fun beginKeyVerificationInDMs(method: String,
                                  transactionId: String,
                                  roomId: String,
                                  otherUserId: String,
                                  otherDeviceId: String,
                                  callback: MatrixCallback<String>?): String?

    fun readyPendingVerificationInDMs(otherUserId: String, roomId: String, transactionId: String)

    // fun transactionUpdated(tx: SasVerificationTransaction)

    interface SasVerificationListener {
        fun transactionCreated(tx: SasVerificationTransaction)
        fun transactionUpdated(tx: SasVerificationTransaction)
        fun markedAsManuallyVerified(userId: String, deviceId: String) {}

        fun verificationRequestCreated(pr: PendingVerificationRequest) {}
        fun verificationRequestUpdated(pr: PendingVerificationRequest) {}
    }

    companion object {

        fun isValidRequest(age: Long?): Boolean {
            if (age == null) return false
            val now = System.currentTimeMillis()
            val tooInThePast = now - (10 * 60 * 1000)
            val fiveMinInMs = 5 * 60 * 1000
            val tooInTheFuture = System.currentTimeMillis() + fiveMinInMs

            return !(age < tooInThePast || age > tooInTheFuture)
        }
    }
}
