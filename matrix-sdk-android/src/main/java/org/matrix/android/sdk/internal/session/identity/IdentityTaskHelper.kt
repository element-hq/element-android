/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.internal.network.executeRequest

internal suspend fun getIdentityApiAndEnsureTerms(identityApiProvider: IdentityApiProvider, userId: String): IdentityAPI {
    val identityAPI = identityApiProvider.identityApi ?: throw IdentityServiceError.NoIdentityServerConfigured

    // Always check that we have access to the service (regarding terms)
    val identityAccountResponse = executeRequest(null) {
        identityAPI.getAccount()
    }

    assert(userId == identityAccountResponse.userId)

    return identityAPI
}
