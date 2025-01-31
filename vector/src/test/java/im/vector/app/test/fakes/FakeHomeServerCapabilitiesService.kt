/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService

class FakeHomeServerCapabilitiesService : HomeServerCapabilitiesService by mockk() {

    fun givenCapabilities(homeServerCapabilities: HomeServerCapabilities) {
        every { getHomeServerCapabilities() } returns homeServerCapabilities
    }
}
