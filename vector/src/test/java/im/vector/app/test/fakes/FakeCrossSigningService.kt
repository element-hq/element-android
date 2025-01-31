/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService

class FakeCrossSigningService : CrossSigningService by mockk() {

    fun givenIsCrossSigningInitializedReturns(isInitialized: Boolean) {
        every { isCrossSigningInitialized() } returns isInitialized
    }

    fun givenIsCrossSigningVerifiedReturns(isVerified: Boolean) {
        every { isCrossSigningVerified() } returns isVerified
    }
}
