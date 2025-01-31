/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.data

import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.session.identity.model.IdentityHashDetailResponse

internal interface IdentityStore {

    fun getIdentityData(): IdentityData?

    fun setUrl(url: String?)

    fun setToken(token: String?)

    fun setUserConsent(consent: Boolean)

    fun setHashDetails(hashDetailResponse: IdentityHashDetailResponse)

    /**
     * Store details about a current binding.
     */
    fun storePendingBinding(threePid: ThreePid, data: IdentityPendingBinding)

    fun getPendingBinding(threePid: ThreePid): IdentityPendingBinding?

    fun deletePendingBinding(threePid: ThreePid)
}

internal fun IdentityStore.getIdentityServerUrlWithoutProtocol(): String? {
    return getIdentityData()?.identityServerUrl?.substringAfter("://")
}
