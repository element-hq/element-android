/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.DiscoveryInformation

object CredentialsFixture {
    fun aCredentials(
            userId: String = "",
            accessToken: String = "",
            refreshToken: String? = null,
            homeServer: String? = null,
            deviceId: String = "VOPOED",
            discoveryInformation: DiscoveryInformation? = null,
    ) = Credentials(
            userId,
            accessToken,
            refreshToken,
            homeServer,
            deviceId,
            discoveryInformation,
    )
}
