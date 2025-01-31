/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.internal.network.token.AccessTokenProvider
import org.matrix.android.sdk.internal.session.identity.data.IdentityStore
import javax.inject.Inject

internal class IdentityAccessTokenProvider @Inject constructor(
        private val identityStore: IdentityStore
) : AccessTokenProvider {
    override fun getToken() = identityStore.getIdentityData()?.token
}
