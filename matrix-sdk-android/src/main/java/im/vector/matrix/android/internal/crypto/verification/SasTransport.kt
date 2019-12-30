/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.matrix.android.internal.crypto.verification

import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState

/**
 * SAS verification can be performed using toDevice events or via DM.
 * This class abstracts the concept of transport for SAS
 */
internal interface SasTransport {

    /**
     * Sends a message
     */
    fun sendToOther(type: String,
                    verificationInfo: VerificationInfo,
                    nextState: SasVerificationTxState,
                    onErrorReason: CancelCode,
                    onDone: (() -> Unit)?)

    fun cancelTransaction(transactionId: String, userId: String, userDevice: String, code: CancelCode)

    fun done(transactionId: String)
    /**
     * Creates an accept message suitable for this transport
     */
    fun createAccept(tid: String,
                     keyAgreementProtocol: String,
                     hash: String,
                     commitment: String,
                     messageAuthenticationCode: String,
                     shortAuthenticationStrings: List<String>): VerificationInfoAccept

    fun createKey(tid: String,
                  pubKey: String): VerificationInfoKey

    fun createStart(fromDevice: String,
                    method: String,
                    transactionID: String,
                    keyAgreementProtocols: List<String>,
                    hashes: List<String>,
                    messageAuthenticationCodes: List<String>,
                    shortAuthenticationStrings: List<String>) : VerificationInfoStart

    fun createMac(tid: String, mac: Map<String, String>, keys: String): VerificationInfoMac


    fun createReady(tid: String, fromDevice: String, methods: List<String>): VerificationInfoReady
}
