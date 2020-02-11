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
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest

/**
 * https://matrix.org/docs/spec/client_server/r0.5.0#key-verification-framework
 *
 * Verifying keys manually by reading out the Ed25519 key is not very user friendly, and can lead to errors.
 * Verification is a user-friendly key verification process.
 * Verification is intended to be a highly interactive process for users,
 * and as such exposes verification methods which are easier for users to use.
 */
interface VerificationService {

    fun addListener(listener: VerificationListener)

    fun removeListener(listener: VerificationListener)

    /**
     * Mark this device as verified manually
     */
    fun markedLocallyAsManuallyVerified(userId: String, deviceID: String)

    fun getExistingTransaction(otherUserId: String, tid: String): VerificationTransaction?

    fun getExistingVerificationRequest(otherUserId: String): List<PendingVerificationRequest>?

    fun getExistingVerificationRequest(otherUserId: String, tid: String?): PendingVerificationRequest?

    fun getExistingVerificationRequestInRoom(roomId: String, tid: String?): PendingVerificationRequest?

    fun beginKeyVerification(method: VerificationMethod,
                             otherUserId: String,
                             otherDeviceId: String,
                             transactionId: String?): String?

    /**
     * Request a key verification from another user using toDevice events.
     */
    fun requestKeyVerificationInDMs(methods: List<VerificationMethod>,
                                    otherUserId: String,
                                    roomId: String,
                                    localId: String? = LocalEcho.createLocalEchoId()): PendingVerificationRequest

    /**
     * Request a key verification from another user using toDevice events.
     */
    fun requestKeyVerification(methods: List<VerificationMethod>,
                               otherUserId: String,
                               otherDevices: List<String>?): PendingVerificationRequest

    fun declineVerificationRequestInDMs(otherUserId: String,
                                        transactionId: String,
                                        roomId: String)

    // Only SAS method is supported for the moment
    // TODO Parameter otherDeviceId should be removed in this case
    fun beginKeyVerificationInDMs(method: VerificationMethod,
                                  transactionId: String,
                                  roomId: String,
                                  otherUserId: String,
                                  otherDeviceId: String,
                                  callback: MatrixCallback<String>?): String?

    /**
     * Returns false if the request is unknown
     */
    fun readyPendingVerificationInDMs(methods: List<VerificationMethod>,
                                      otherUserId: String,
                                      roomId: String,
                                      transactionId: String): Boolean

    /**
     * Returns false if the request is unknown
     */
    fun readyPendingVerification(methods: List<VerificationMethod>,
                                 otherUserId: String,
                                 transactionId: String): Boolean

    // fun transactionUpdated(tx: SasVerificationTransaction)

    interface VerificationListener {
        fun transactionCreated(tx: VerificationTransaction)
        fun transactionUpdated(tx: VerificationTransaction)
        fun markedAsManuallyVerified(userId: String, deviceId: String) {}

        fun verificationRequestCreated(pr: PendingVerificationRequest) {}
        fun verificationRequestUpdated(pr: PendingVerificationRequest) {}
    }

    companion object {

        private const val TEN_MINUTES_IN_MILLIS = 10 * 60 * 1000
        private const val FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000

        fun isValidRequest(age: Long?): Boolean {
            if (age == null) return false
            val now = System.currentTimeMillis()
            val tooInThePast = now - TEN_MINUTES_IN_MILLIS
            val tooInTheFuture = now + FIVE_MINUTES_IN_MILLIS
            return age in tooInThePast..tooInTheFuture
        }
    }
}
