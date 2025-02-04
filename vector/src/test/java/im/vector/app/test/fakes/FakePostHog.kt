/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.os.Looper
import com.posthog.PostHogInterface
import im.vector.app.features.analytics.plan.UserProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify

class FakePostHog {

    init {
        // workaround to avoid PostHog.HANDLER failing
        mockkStatic(Looper::class)
        val looper = mockk<Looper> {
            every { thread } returns Thread.currentThread()
        }
        every { Looper.getMainLooper() } returns looper
    }

    val instance = mockk<PostHogInterface>(relaxed = true)

    fun verifyOptOutStatus(optedOut: Boolean) {
        if (optedOut) {
            verify { instance.optOut() }
        } else {
            verify { instance.optIn() }
        }
    }

    fun verifyIdentifies(analyticsId: String, userProperties: UserProperties?) {
        verify {
            val postHogProperties = userProperties?.getProperties()
                    ?.takeIf { it.isNotEmpty() }
            instance.identify(analyticsId, postHogProperties, null)
        }
    }

    fun verifyReset() {
        verify { instance.reset() }
    }

    fun verifyScreenTracked(name: String, properties: Map<String, Any>?) {
        verify { instance.screen(name, properties) }
    }

    fun verifyNoScreenTracking() {
        verify(exactly = 0) {
            instance.screen(any())
            instance.screen(any(), any())
        }
    }

    fun verifyEventTracked(name: String, properties: Map<String, Any>?) {
        verify { instance.capture(name, null, properties) }
    }

    fun verifyNoEventTracking() {
        verify(exactly = 0) {
            instance.capture(any())
            instance.capture(any(), any())
            instance.capture(any(), any(), any())
        }
    }
}
