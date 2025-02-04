/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.resources.DrawableProvider
import io.mockk.every
import io.mockk.mockk

class FakeDrawableProvider {
    val instance = mockk<DrawableProvider>()

    init {
        every { instance.getDrawable(any()) } returns mockk()
        every { instance.getDrawable(any(), any()) } returns mockk()
    }
}
