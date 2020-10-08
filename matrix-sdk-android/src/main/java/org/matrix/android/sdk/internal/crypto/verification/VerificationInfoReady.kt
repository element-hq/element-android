/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoReady

/**
 * A new event type is added to the key verification framework: m.key.verification.ready,
 * which may be sent by the target of the m.key.verification.request message, upon receipt of the m.key.verification.request event.
 *
 * The m.key.verification.ready event is optional; the recipient of the m.key.verification.request event may respond directly
 * with a m.key.verification.start event instead.
 */

internal interface VerificationInfoReady : VerificationInfo<ValidVerificationInfoReady> {
    /**
     * The ID of the device that sent the m.key.verification.ready message
     */
    val fromDevice: String?

    /**
     * An array of verification methods that the device supports
     */
    val methods: List<String>?

    override fun asValidObject(): ValidVerificationInfoReady? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validFromDevice = fromDevice?.takeIf { it.isNotEmpty() } ?: return null
        val validMethods = methods?.takeIf { it.isNotEmpty() } ?: return null

        return ValidVerificationInfoReady(
                validTransactionId,
                validFromDevice,
                validMethods
        )
    }
}

internal interface MessageVerificationReadyFactory {
    fun create(tid: String, methods: List<String>, fromDevice: String): VerificationInfoReady
}
