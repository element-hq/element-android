/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShowVerifyDeviceBannerCheckerTest {
    private val checker = ShowVerifyDeviceBannerChecker()

    @Test
    fun `should show banner for non excluded domain`() {
        val userId = "@user:example.com"
        assertTrue(checker.canShowVerifyDeviceBanner(userId))
    }

    @Test
    fun `should show banner for matrix_org users`() {
        val userId = "@user:matrix.org"
        assertTrue(checker.canShowVerifyDeviceBanner(userId))
    }
}
