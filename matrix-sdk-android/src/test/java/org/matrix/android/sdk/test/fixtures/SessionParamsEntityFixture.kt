/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fixtures

import org.matrix.android.sdk.internal.auth.db.SessionParamsEntity

internal object SessionParamsEntityFixture {
    fun aSessionParamsEntity(
            sessionId: String = "",
            userId: String = "",
            credentialsJson: String = "",
            homeServerConnectionConfigJson: String = "",
            isTokenValid: Boolean = true,
            loginType: String = "",
    ) = SessionParamsEntity(
            sessionId,
            userId,
            credentialsJson,
            homeServerConnectionConfigJson,
            isTokenValid,
            loginType,
    )
}
