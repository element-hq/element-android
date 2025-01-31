/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network.token

import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.di.SessionId
import javax.inject.Inject

internal class HomeserverAccessTokenProvider @Inject constructor(
        @SessionId private val sessionId: String,
        private val sessionParamsStore: SessionParamsStore
) : AccessTokenProvider {
    override fun getToken() = sessionParamsStore.get(sessionId)?.credentials?.accessToken
}
