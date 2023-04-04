/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.crypto.keysbackup

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP

data class KeyBackupConfig(
        val defaultAlgorithm: String = MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP,
        val supportedAlgorithms: List<String> = listOf(MXCRYPTO_ALGORITHM_CURVE_25519_BACKUP)
) {
    fun isAlgorithmSupported(alg: String) = supportedAlgorithms.contains(alg)
}
