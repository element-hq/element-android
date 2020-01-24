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
package im.vector.matrix.android.internal.crypto.verification

import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.VerificationTransaction

/**
 * Generic interactive key verification transaction
 */
internal abstract class DefaultVerificationTransaction(
        override val transactionId: String,
        override val otherUserId: String,
        override var otherDeviceId: String? = null,
        override val isIncoming: Boolean) : VerificationTransaction {

    lateinit var transport: SasTransport

    interface Listener {
        fun transactionUpdated(tx: VerificationTransaction)
    }

    protected var listeners = ArrayList<Listener>()

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    abstract fun acceptVerificationEvent(senderId: String, info: VerificationInfo)
}
