/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import com.posthog.android.PostHog
import im.vector.app.features.analytics.impl.PostHogFactory
import io.mockk.every
import io.mockk.mockk

class FakePostHogFactory(postHog: PostHog) {
    val instance = mockk<PostHogFactory>().also {
        every { it.createPosthog() } returns postHog
    }
}
