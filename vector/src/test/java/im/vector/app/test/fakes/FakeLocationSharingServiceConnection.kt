/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.location.live.tracking.LocationSharingServiceConnection
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class FakeLocationSharingServiceConnection {

    val instance = mockk<LocationSharingServiceConnection>()

    fun givenBind() {
        every { instance.bind(any()) } just runs
    }

    fun verifyBind(callback: LocationSharingServiceConnection.Callback) {
        verify { instance.bind(callback) }
    }
}
