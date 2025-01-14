/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
