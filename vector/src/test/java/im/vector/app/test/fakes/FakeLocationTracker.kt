/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.location.LocationTracker
import io.mockk.mockk
import io.mockk.verify

class FakeLocationTracker {

    val instance: LocationTracker = mockk(relaxed = true)

    fun verifyAddCallback(callback: LocationTracker.Callback) {
        verify { instance.addCallback(callback) }
    }

    fun verifyRemoveCallback(callback: LocationTracker.Callback) {
        verify { instance.removeCallback(callback) }
    }
}
