/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
