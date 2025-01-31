/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.homeserver

import org.matrix.android.sdk.internal.auth.version.Versions
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET

internal interface CapabilitiesAPI {
    /**
     * Request the homeserver capabilities.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "capabilities")
    suspend fun getCapabilities(): GetCapabilitiesResult

    /**
     * Request the versions.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_ + "versions")
    suspend fun getVersions(): Versions

    /**
     * Ping the homeserver. We do not care about the returned data, so there is no use to parse them.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_ + "versions")
    suspend fun ping()
}
