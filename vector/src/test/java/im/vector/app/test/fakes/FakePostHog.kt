/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.os.Looper
import com.posthog.android.PostHog
import com.posthog.android.Properties
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

    val instance = mockk<PostHog>(relaxed = true)

    fun verifyOptOutStatus(optedOut: Boolean) {
        verify { instance.optOut(optedOut) }
    }

    fun verifyIdentifies(analyticsId: String, userProperties: UserProperties?) {
        verify {
            val postHogProperties = userProperties?.getProperties()
                    ?.let { rawProperties -> Properties().also { it.putAll(rawProperties) } }
                    ?.takeIf { it.isNotEmpty() }
            instance.identify(analyticsId, postHogProperties, null)
        }
    }

    fun verifyReset() {
        verify { instance.reset() }
    }

    fun verifyScreenTracked(name: String, properties: Properties?) {
        verify { instance.screen(name, properties) }
    }

    fun verifyNoScreenTracking() {
        verify(exactly = 0) {
            instance.screen(any())
            instance.screen(any(), any())
            instance.screen(any(), any(), any())
        }
    }

    fun verifyEventTracked(name: String, properties: Properties?) {
        verify { instance.capture(name, properties) }
    }

    fun verifyNoEventTracking() {
        verify(exactly = 0) {
            instance.capture(any())
            instance.capture(any(), any())
            instance.capture(any(), any(), any())
        }
    }
}
