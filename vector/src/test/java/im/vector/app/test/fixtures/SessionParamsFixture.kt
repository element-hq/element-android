/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.test.fixtures.CredentialsFixture.aCredentials
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams

object SessionParamsFixture {
    fun aSessionParams(
            credentials: Credentials = aCredentials(),
            homeServerConnectionConfig: HomeServerConnectionConfig = mockk(relaxed = true),
            isTokenValid: Boolean = false,
            loginType: LoginType = LoginType.UNKNOWN,
    ) = SessionParams(
            credentials,
            homeServerConnectionConfig,
            isTokenValid,
            loginType,
    )
}
