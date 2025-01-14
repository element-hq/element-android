/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.location.live.tracking.LocationSharingServiceConnection
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class FakeLocationSharingServiceConnection {

    val instance = mockk<LocationSharingServiceConnection>()

    fun givenBind() {
        justRun { instance.bind(any()) }
    }

    fun verifyBind(callback: LocationSharingServiceConnection.Callback) {
        verify { instance.bind(callback) }
    }

    fun givenUnbind() {
        justRun { instance.unbind(any()) }
    }

    fun verifyUnbind(callback: LocationSharingServiceConnection.Callback) {
        verify { instance.unbind(callback) }
    }
}
