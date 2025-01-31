/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fixtures

import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.test.fixtures.CredentialsFixture.aCredentials

object SessionParamsFixture {
    fun aSessionParams(
            credentials: Credentials = aCredentials(),
            homeServerConnectionConfig: HomeServerConnectionConfig = HomeServerConnectionConfig.Builder().withHomeServerUri("homeserver").build(),
            isTokenValid: Boolean = false,
            loginType: LoginType = LoginType.UNKNOWN,
    ) = SessionParams(
            credentials,
            homeServerConnectionConfig,
            isTokenValid,
            loginType,
    )
}
