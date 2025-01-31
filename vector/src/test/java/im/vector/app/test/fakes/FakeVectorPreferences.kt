/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.settings.VectorPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FakeVectorPreferences {

    val instance = mockk<VectorPreferences>(relaxUnitFun = true)

    fun givenUseCompleteNotificationFormat(value: Boolean) {
        every { instance.useCompleteNotificationFormat() } returns value
    }

    fun givenSpaceBackstack(value: List<String?>) {
        every { instance.getSpaceBackstack() } returns value
    }

    fun verifySetSpaceBackstack(value: List<String?>, inverse: Boolean = false) {
        verify(inverse = inverse) { instance.setSpaceBackstack(value) }
    }
}
