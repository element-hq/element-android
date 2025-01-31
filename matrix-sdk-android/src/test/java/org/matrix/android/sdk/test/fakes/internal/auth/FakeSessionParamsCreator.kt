/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.test.fakes.internal.auth

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.auth.SessionParamsCreator
import org.matrix.android.sdk.test.fixtures.SessionParamsFixture.aSessionParams

internal class FakeSessionParamsCreator {

    val instance: SessionParamsCreator = mockk()

    init {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
        coEvery { instance.create(any(), any(), any()) } returns sessionParams
    }

    fun verifyCreatedWithParameters(
            credentials: Credentials,
            homeServerConnectionConfig: HomeServerConnectionConfig,
            loginType: LoginType,
    ) {
        coVerify { instance.create(credentials, homeServerConnectionConfig, loginType) }
    }

    companion object {
        val sessionParams = aSessionParams()
    }
}
