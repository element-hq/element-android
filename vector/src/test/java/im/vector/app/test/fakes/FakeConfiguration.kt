/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.configuration.VectorConfiguration
import io.mockk.every
import io.mockk.mockk

class FakeConfiguration {

    val instance = mockk<VectorConfiguration> {
        every { applyToApplicationContext() } returns Unit
    }
}
