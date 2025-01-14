/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.liveData
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.matrix.android.sdk.api.session.pushers.HttpPusher
import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.api.session.pushers.PushersService

class FakePushersService : PushersService by mockk(relaxed = true) {

    fun givenGetPushers(pushers: List<Pusher>) {
        every { getPushers() } returns pushers
    }

    fun givenPushersLive(pushers: List<Pusher>) {
        every { getPushersLive() } returns liveData { emit(pushers) }
    }

    fun verifyTogglePusherCalled(pusher: Pusher, enable: Boolean) {
        coVerify {
            togglePusher(pusher, enable)
        }
    }

    fun verifyEnqueueAddHttpPusher(): HttpPusher {
        val httpPusherSlot = slot<HttpPusher>()
        verify { enqueueAddHttpPusher(capture(httpPusherSlot)) }
        return httpPusherSlot.captured
    }

    fun givenRefreshPushersSucceeds() {
        justRun { refreshPushers() }
    }

    fun verifyRefreshPushers() {
        verify { refreshPushers() }
    }
}
