/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.SpaceStateHandler
import io.mockk.mockk
import io.mockk.verify

class FakeSpaceStateHandler : SpaceStateHandler by mockk(relaxUnitFun = true) {

    fun verifySetCurrentSpace(spaceId: String) {
        verify { setCurrentSpace(spaceId) }
    }
}
