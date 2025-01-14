/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.pushers.UnifiedPushStore
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class FakeUnifiedPushStore {

    val instance = mockk<UnifiedPushStore>()

    fun givenStoreUpEndpoint(endpoint: String?) {
        justRun { instance.storeUpEndpoint(endpoint) }
    }

    fun verifyStoreUpEndpoint(endpoint: String?) {
        verify { instance.storeUpEndpoint(endpoint) }
    }

    fun givenStorePushGateway(gateway: String?) {
        justRun { instance.storePushGateway(gateway) }
    }

    fun verifyStorePushGateway(gateway: String?) {
        verify { instance.storePushGateway(gateway) }
    }
}
