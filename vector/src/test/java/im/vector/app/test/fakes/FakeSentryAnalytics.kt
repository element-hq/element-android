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

import im.vector.app.features.analytics.impl.SentryAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FakeSentryAnalytics {
    private var isSentryEnabled = false

    val instance = mockk<SentryAnalytics>(relaxUnitFun = true).also {
        every { it.initSentry() } answers  {
            isSentryEnabled = true
        }

        every { it.stopSentry() } answers {
            isSentryEnabled = false
        }
    }

    fun verifySentryInit() {
        verify { instance.initSentry() }
    }

    fun verifySentryClose() {
        verify { instance.stopSentry() }
    }

    fun verifySentryTrackError(error: Throwable) {
        verify { instance.trackError(error) }
    }

    fun verifyNoErrorTracking() =
        verify(inverse = true) {
            instance.trackError(any())
        }
}
