/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.settings.devices.v2.notification.ToggleNotificationsUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class FakeToggleNotificationUseCase {

    val instance = mockk<ToggleNotificationsUseCase> {
        coJustRun { execute(any(), any()) }
    }

    fun verifyExecute(deviceId: String, enabled: Boolean) {
        coVerify { instance.execute(deviceId, enabled) }
    }
}
