/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.test.fakes

import com.posthog.PostHogInterface
import im.vector.app.features.analytics.impl.PostHogFactory
import io.mockk.every
import io.mockk.mockk

class FakePostHogFactory(postHog: PostHogInterface) {
    val instance = mockk<PostHogFactory>().also {
        every { it.createPosthog() } returns postHog
    }
}
