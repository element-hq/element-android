/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup

import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestData
import org.matrix.android.sdk.internal.crypto.model.MXInboundMegolmSessionWrapper

/**
 * Data class to store result of [KeysBackupTestHelper.createKeysBackupScenarioWithPassword]
 */
internal data class KeysBackupScenarioData(
        val cryptoTestData: CryptoTestData,
        val aliceKeys: List<MXInboundMegolmSessionWrapper>,
        val prepareKeysBackupDataResult: PrepareKeysBackupDataResult,
        val aliceSession2: Session
) {
    fun cleanUp(testHelper: CommonTestHelper) {
        cryptoTestData.cleanUp(testHelper)
        testHelper.signOutAndClose(aliceSession2)
    }
}
