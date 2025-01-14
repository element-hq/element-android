/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.pushers.UnifiedPushHelper
import io.mockk.every
import io.mockk.mockk

class FakeUnifiedPushHelper {

    val instance = mockk<UnifiedPushHelper>()

    fun givenIsEmbeddedDistributorReturns(isEmbedded: Boolean) {
        every { instance.isEmbeddedDistributor() } returns isEmbedded
    }

    fun givenGetEndpointOrTokenReturns(endpoint: String?) {
        every { instance.getEndpointOrToken() } returns endpoint
    }

    fun givenIsBackgroundSyncReturns(enabled: Boolean) {
        every { instance.isBackgroundSync() } returns enabled
    }
}
