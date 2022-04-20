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
            else                   -> false
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
            else                   -> false
        }
    }

    /**
     * @return The list of registered algorithms.
     */
    fun supportedAlgorithms(): List<String> {
        return listOf(MXCRYPTO_ALGORITHM_MEGOLM, MXCRYPTO_ALGORITHM_OLM)
    }
}
