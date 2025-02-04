/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.login.ReAuthHelper
import io.mockk.every
import io.mockk.mockk

class FakeReAuthHelper {

    val instance = mockk<ReAuthHelper>()

    fun givenStoredPassword(pwd: String?) {
        every { instance.data } returns pwd
    }
}
