/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.pushers.PushersManager
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.pushers.Pusher

class FakePushersManager {

    val instance = mockk<PushersManager>()

    fun givenGetPusherForCurrentSessionReturns(pusher: Pusher?) {
        every { instance.getPusherForCurrentSession() } returns pusher
    }

    fun givenUnregisterPusher(pushKey: String) {
        coJustRun { instance.unregisterPusher(pushKey) }
    }

    fun verifyUnregisterPusher(pushKey: String) {
        coVerify { instance.unregisterPusher(pushKey) }
    }
}
