/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.common

import org.matrix.android.sdk.api.session.Session

data class CryptoTestData(
        val roomId: String,
        val sessions: List<Session>
) {

    val firstSession: Session
        get() = sessions.first()

    val secondSession: Session?
        get() = sessions.getOrNull(1)

    val thirdSession: Session?
        get() = sessions.getOrNull(2)

    fun cleanUp(testHelper: CommonTestHelper) {
        sessions.forEach {
            testHelper.signOutAndClose(it)
        }
    }
}
