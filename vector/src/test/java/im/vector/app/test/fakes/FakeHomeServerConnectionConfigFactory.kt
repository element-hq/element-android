/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.login.HomeServerConnectionConfigFactory
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.network.ssl.Fingerprint

class FakeHomeServerConnectionConfigFactory {
    val instance: HomeServerConnectionConfigFactory = mockk()

    fun givenConfigFor(url: String, fingerprints: List<Fingerprint>? = null, config: HomeServerConnectionConfig) {
        every { instance.create(url, fingerprints) } returns config
    }
}
