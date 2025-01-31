/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.media

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET
import retrofit2.http.Query

internal interface MediaAPI {
    /**
     * Retrieve the configuration of the content repository
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#get-matrix-media-r0-config
     */
    @GET(NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0 + "config")
    suspend fun getMediaConfig(): GetMediaConfigResult

    /**
     * Get information about a URL for the client. Typically this is called when a client
     * sees a URL in a message and wants to render a preview for the user.
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#get-matrix-media-r0-preview-url
     * @param url Required. The URL to get a preview of.
     * @param ts The preferred point in time to return a preview for. The server may return a newer version
     * if it does not have the requested version available.
     */
    @GET(NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0 + "preview_url")
    suspend fun getPreviewUrlData(@Query("url") url: String, @Query("ts") ts: Long?): JsonDict
}
