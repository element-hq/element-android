/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto

import android.text.TextUtils
import im.vector.matrix.android.internal.crypto.algorithms.IMXDecrypting
import im.vector.matrix.android.internal.crypto.algorithms.IMXEncrypting
import timber.log.Timber
import java.util.*

internal object MXCryptoAlgorithms {

    // encryptors map
    private val mEncryptors: MutableMap<String, Class<IMXEncrypting>>

    // decryptors map
    private val mDecryptors: MutableMap<String, Class<IMXDecrypting>>

    init {
        mEncryptors = HashMap()
        try {
            mEncryptors[MXCRYPTO_ALGORITHM_MEGOLM] = Class.forName("im.vector.matrix.android.internal.crypto.algorithms.megolm.MXMegolmEncryption") as Class<IMXEncrypting>
        } catch (e: Exception) {
            Timber.e("## MXCryptoAlgorithms() : fails to add MXCRYPTO_ALGORITHM_MEGOLM")
        }

        try {
            mEncryptors[MXCRYPTO_ALGORITHM_OLM] = Class.forName("im.vector.matrix.android.internal.crypto.algorithms.olm.MXOlmEncryption") as Class<IMXEncrypting>
        } catch (e: Exception) {
            Timber.e("## MXCryptoAlgorithms() : fails to add MXCRYPTO_ALGORITHM_OLM")
        }

        mDecryptors = HashMap()
        try {
            mDecryptors[MXCRYPTO_ALGORITHM_MEGOLM] = Class.forName("im.vector.matrix.android.internal.crypto.algorithms.megolm.MXMegolmDecryption") as Class<IMXDecrypting>
        } catch (e: Exception) {
            Timber.e("## MXCryptoAlgorithms() : fails to add MXCRYPTO_ALGORITHM_MEGOLM")
        }

        try {
            mDecryptors[MXCRYPTO_ALGORITHM_OLM] = Class.forName("im.vector.matrix.android.internal.crypto.algorithms.olm.MXOlmDecryption") as Class<IMXDecrypting>
        } catch (e: Exception) {
            Timber.e("## MXCryptoAlgorithms() : fails to add MXCRYPTO_ALGORITHM_OLM")
        }

    }

    /**
     * Get the class implementing encryption for the provided algorithm.
     *
     * @param algorithm the algorithm tag.
     * @return A class implementing 'IMXEncrypting'.
     */
    fun encryptorClassForAlgorithm(algorithm: String?): Class<IMXEncrypting>? {
        return if (!TextUtils.isEmpty(algorithm)) {
            mEncryptors[algorithm]
        } else {
            null
        }
    }

    /**
     * Get the class implementing decryption for the provided algorithm.
     *
     * @param algorithm the algorithm tag.
     * @return A class implementing 'IMXDecrypting'.
     */

    fun decryptorClassForAlgorithm(algorithm: String?): Class<IMXDecrypting>? {
        return if (!TextUtils.isEmpty(algorithm)) {
            mDecryptors[algorithm]
        } else {
            null
        }
    }

    /**
     * @return The list of registered algorithms.
     */
    fun supportedAlgorithms(): List<String> {
        return ArrayList(mEncryptors.keys)
    }
}