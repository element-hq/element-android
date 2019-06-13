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

interface SasVerificationService {
    fun addListener(listener: SasVerificationListener)

    fun removeListener(listener: SasVerificationListener)

    fun markedLocallyAsManuallyVerified(userId: String, deviceID: String)

    fun getExistingTransaction(otherUser: String, tid: String): SasVerificationTransaction?

    fun beginKeyVerificationSAS(userId: String, deviceID: String): String?

    fun beginKeyVerification(method: String, userId: String, deviceID: String): String?

    // fun transactionUpdated(tx: SasVerificationTransaction)

    interface SasVerificationListener {
        fun transactionCreated(tx: SasVerificationTransaction)
        fun transactionUpdated(tx: SasVerificationTransaction)
        fun markedAsManuallyVerified(userId: String, deviceId: String)
    }
}