/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.openid

import org.matrix.android.sdk.api.session.openid.OpenIdService
import org.matrix.android.sdk.api.session.openid.OpenIdToken
import javax.inject.Inject

internal class DefaultOpenIdService @Inject constructor(private val getOpenIdTokenTask: GetOpenIdTokenTask) : OpenIdService {

    override suspend fun getOpenIdToken(): OpenIdToken {
        return getOpenIdTokenTask.execute(Unit)
    }
}
