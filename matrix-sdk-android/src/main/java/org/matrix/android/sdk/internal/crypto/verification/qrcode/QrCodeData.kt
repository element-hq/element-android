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

package org.matrix.android.sdk.internal.crypto.verification.qrcode

/**
 * Ref: https://github.com/uhoreg/matrix-doc/blob/qr_key_verification/proposals/1543-qr_code_key_verification.md#qr-code-format
 */
internal sealed class QrCodeData(
        /**
         * the event ID or transaction_id of the associated verification
         */
        open val transactionId: String,
        /**
         * First key (32 bytes, in base64 no padding)
         */
        val firstKey: String,
        /**
         * Second key (32 bytes, in base64 no padding)
         */
        val secondKey: String,
        /**
         * a random shared secret (in base64 no padding)
         */
        open val sharedSecret: String
) {
    /**
     * verifying another user with cross-signing
     * QR code verification mode: 0x00
     */
    data class VerifyingAnotherUser(
            override val transactionId: String,
            /**
             * the user's own master cross-signing public key
             */
            val userMasterCrossSigningPublicKey: String,
            /**
             * what the device thinks the other user's master cross-signing key is
             */
            val otherUserMasterCrossSigningPublicKey: String,
            override val sharedSecret: String
    ) : QrCodeData(
            transactionId,
            userMasterCrossSigningPublicKey,
            otherUserMasterCrossSigningPublicKey,
            sharedSecret)

    /**
     * self-verifying in which the current device does trust the master key
     * QR code verification mode: 0x01
     */
    data class SelfVerifyingMasterKeyTrusted(
            override val transactionId: String,
            /**
             * the user's own master cross-signing public key
             */
            val userMasterCrossSigningPublicKey: String,
            /**
             * what the device thinks the other device's device key is
             */
            val otherDeviceKey: String,
            override val sharedSecret: String
    ) : QrCodeData(
            transactionId,
            userMasterCrossSigningPublicKey,
            otherDeviceKey,
            sharedSecret)

    /**
     * self-verifying in which the current device does not yet trust the master key
     * QR code verification mode: 0x02
     */
    data class SelfVerifyingMasterKeyNotTrusted(
            override val transactionId: String,
            /**
             * the current device's device key
             */
            val deviceKey: String,
            /**
             * what the device thinks the user's master cross-signing key is
             */
            val userMasterCrossSigningPublicKey: String,
            override val sharedSecret: String
    ) : QrCodeData(
            transactionId,
            deviceKey,
            userMasterCrossSigningPublicKey,
            sharedSecret)
}
