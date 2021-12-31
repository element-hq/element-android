/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState

/**
 * Verification can be performed using toDevice events or via DM.
 * This class abstracts the concept of transport for verification
 */
internal interface VerificationTransport {

    /**
     * Sends a message
     */
    fun <T> sendToOther(type: String,
                        verificationInfo: VerificationInfo<T>,
                        nextState: VerificationTxState,
                        onErrorReason: CancelCode,
                        onDone: (() -> Unit)?)

    /**
     * @param callback will be called with eventId and ValidVerificationInfoRequest in case of success
     */
    fun sendVerificationRequest(supportedMethods: List<String>,
                                localId: String,
                                otherUserId: String,
                                roomId: String?,
                                toDevices: List<String>?,
                                callback: (String?, ValidVerificationInfoRequest?) -> Unit)

    fun cancelTransaction(transactionId: String,
                          otherUserId: String,
                          otherUserDeviceId: String?,
                          code: CancelCode)

    fun done(transactionId: String,
             onDone: (() -> Unit)?)

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

    /**
     * Create start for SAS verification
     */
    fun createStartForSas(fromDevice: String,
                          transactionId: String,
                          keyAgreementProtocols: List<String>,
                          hashes: List<String>,
                          messageAuthenticationCodes: List<String>,
                          shortAuthenticationStrings: List<String>): VerificationInfoStart

    /**
     * Create start for QR code verification
     */
    fun createStartForQrCode(fromDevice: String,
                             transactionId: String,
                             sharedSecret: String): VerificationInfoStart

    fun createMac(tid: String, mac: Map<String, String>, keys: String): VerificationInfoMac

    fun createReady(tid: String,
                    fromDevice: String,
                    methods: List<String>): VerificationInfoReady

    // TODO Refactor
    fun sendVerificationReady(keyReq: VerificationInfoReady,
                              otherUserId: String,
                              otherDeviceId: String?,
                              callback: (() -> Unit)?)
}
