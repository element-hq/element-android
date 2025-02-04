/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.lib.core.utils.timer.Clock
import io.mockk.every
import io.mockk.mockk

class FakeClock : Clock by mockk() {
    fun givenEpoch(epoch: Long) {
        every { epochMillis() } returns epoch
    }
}
