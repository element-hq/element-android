/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.api.session.crypto.verification.SasMode
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS

internal interface VerificationInfoStart : VerificationInfo<ValidVerificationInfoStart> {

    val method: String?

    /**
     * Alice’s device ID.
     */
    val fromDevice: String?

    /**
     * An array of key agreement protocols that Alice’s client understands.
     * Must include “curve25519”.
     * Other methods may be defined in the future
     */
    val keyAgreementProtocols: List<String>?

    /**
     * An array of hashes that Alice’s client understands.
     * Must include “sha256”.  Other methods may be defined in the future.
     */
    val hashes: List<String>?

    /**
     * An array of message authentication codes that Alice’s client understands.
     * Must include “hkdf-hmac-sha256”.
     * Other methods may be defined in the future.
     */
    val messageAuthenticationCodes: List<String>?

    /**
     * An array of short authentication string methods that Alice’s client (and Alice) understands.
     * Must include “decimal”.
     * This document also describes the “emoji” method.
     * Other methods may be defined in the future
     */
    val shortAuthenticationStrings: List<String>?

    /**
     * Shared secret, when starting verification with QR code.
     */
    val sharedSecret: String?

    fun toCanonicalJson(): String

    override fun asValidObject(): ValidVerificationInfoStart? {
        val validTransactionId = transactionId?.takeIf { it.isNotEmpty() } ?: return null
        val validFromDevice = fromDevice?.takeIf { it.isNotEmpty() } ?: return null

        return when (method) {
            VERIFICATION_METHOD_SAS -> {
                val validKeyAgreementProtocols = keyAgreementProtocols?.takeIf { it.isNotEmpty() } ?: return null
                val validHashes = hashes?.takeIf { it.contains("sha256") } ?: return null
                val validMessageAuthenticationCodes = messageAuthenticationCodes
                        ?.takeIf {
                            it.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256) ||
                                    it.contains(SASDefaultVerificationTransaction.SAS_MAC_SHA256_LONGKDF)
                        }
                        ?: return null
                val validShortAuthenticationStrings = shortAuthenticationStrings?.takeIf { it.contains(SasMode.DECIMAL) } ?: return null

                ValidVerificationInfoStart.SasVerificationInfoStart(
                        validTransactionId,
                        validFromDevice,
                        validKeyAgreementProtocols,
                        validHashes,
                        validMessageAuthenticationCodes,
                        validShortAuthenticationStrings,
                        canonicalJson = toCanonicalJson()
                )
            }
            VERIFICATION_METHOD_RECIPROCATE -> {
                val validSharedSecret = sharedSecret?.takeIf { it.isNotEmpty() } ?: return null

                ValidVerificationInfoStart.ReciprocateVerificationInfoStart(
                        validTransactionId,
                        validFromDevice,
                        validSharedSecret
                )
            }
            else -> null
        }
    }
}

internal sealed class ValidVerificationInfoStart(
        open val transactionId: String,
        open val fromDevice: String
) {
    data class SasVerificationInfoStart(
            override val transactionId: String,
            override val fromDevice: String,
            val keyAgreementProtocols: List<String>,
            val hashes: List<String>,
            val messageAuthenticationCodes: List<String>,
            val shortAuthenticationStrings: List<String>,
            val canonicalJson: String
    ) : ValidVerificationInfoStart(transactionId, fromDevice)

    data class ReciprocateVerificationInfoStart(
            override val transactionId: String,
            override val fromDevice: String,
            val sharedSecret: String
    ) : ValidVerificationInfoStart(transactionId, fromDevice)
}
