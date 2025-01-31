/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_OLM

// TODO Update comment
internal object MXCryptoAlgorithms {

    /**
     * Get the class implementing encryption for the provided algorithm.
     *
     * @param algorithm the algorithm tag.
     * @return A class implementing 'IMXEncrypting'.
     */
    fun hasEncryptorClassForAlgorithm(algorithm: String?): Boolean {
        return when (algorithm) {
            MXCRYPTO_ALGORITHM_MEGOLM,
            MXCRYPTO_ALGORITHM_OLM -> true
            else -> false
        }
    }

    /**
     * Get the class implementing decryption for the provided algorithm.
     *
     * @param algorithm the algorithm tag.
     * @return A class implementing 'IMXDecrypting'.
     */

    fun hasDecryptorClassForAlgorithm(algorithm: String?): Boolean {
        return when (algorithm) {
            MXCRYPTO_ALGORITHM_MEGOLM,
            MXCRYPTO_ALGORITHM_OLM -> true
            else -> false
        }
    }

    /**
     * @return The list of registered algorithms.
     */
    fun supportedAlgorithms(): List<String> {
        return listOf(MXCRYPTO_ALGORITHM_MEGOLM, MXCRYPTO_ALGORITHM_OLM)
    }
}
