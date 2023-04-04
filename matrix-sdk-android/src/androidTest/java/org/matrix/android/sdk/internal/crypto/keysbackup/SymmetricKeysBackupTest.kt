/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License atcle
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup

import androidx.test.filters.LargeTest
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_AES_256_BACKUP
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeyBackupConfig

@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class SymmetricKeysBackupTest : KeysBackupTest() {

    override var keyBackupConfig: KeyBackupConfig? = KeyBackupConfig(
            defaultAlgorithm = MXCRYPTO_ALGORITHM_AES_256_BACKUP,
            supportedAlgorithms = listOf(MXCRYPTO_ALGORITHM_AES_256_BACKUP)
    )
}
