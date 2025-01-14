/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.analytics.impl.LateInitUserPropertiesFactory
import im.vector.app.features.analytics.plan.UserProperties
import io.mockk.coEvery
import io.mockk.mockk

class FakeLateInitUserPropertiesFactory {

    val instance = mockk<LateInitUserPropertiesFactory>()

    fun givenCreatesProperties(userProperties: UserProperties?) {
        coEvery { instance.createUserProperties() } returns userProperties
    }
}
