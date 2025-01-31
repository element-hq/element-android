/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.federation

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET

internal interface FederationAPI {
    @GET(NetworkConstants.URI_FEDERATION_PATH + "version")
    suspend fun getVersion(): FederationGetVersionResult
}
