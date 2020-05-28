/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.keysbackup

import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.CryptoTestData
import im.vector.matrix.android.internal.crypto.model.OlmInboundGroupSessionWrapper2

/**
 * Data class to store result of [KeysBackupTestHelper.createKeysBackupScenarioWithPassword]
 */
data class KeysBackupScenarioData(val cryptoTestData: CryptoTestData,
                                  val aliceKeys: List<OlmInboundGroupSessionWrapper2>,
                                  val prepareKeysBackupDataResult: PrepareKeysBackupDataResult,
                                  val aliceSession2: Session) {
    fun cleanUp(testHelper: CommonTestHelper) {
        cryptoTestData.cleanUp(testHelper)
        testHelper.signOutAndClose(aliceSession2)
    }
}
