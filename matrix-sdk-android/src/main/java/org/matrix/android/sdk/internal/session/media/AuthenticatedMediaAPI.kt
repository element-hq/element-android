/*
 * Copyright (C) 2024 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.media

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Implementation of the media repository API using the new Authenticated media API.
 */
internal interface AuthenticatedMediaAPI : MediaAPI {
    /**
     * Retrieve the configuration of the content repository
     * Ref: https://spec.matrix.org/v1.11/client-server-api/#get_matrixclientv1mediaconfig
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_V1 + "media/config")
    override suspend fun getMediaConfig(): GetMediaConfigResult

    /**
     * Get information about a URL for the client. Typically this is called when a client
     * sees a URL in a message and wants to render a preview for the user.
     * Ref: https://spec.matrix.org/v1.11/client-server-api/#get_matrixclientv1mediapreview_url
     * @param url Required. The URL to get a preview of.
     * @param ts The preferred point in time to return a preview for. The server may return a newer version
     * if it does not have the requested version available.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_V1 + "media/preview_url")
    override suspend fun getPreviewUrlData(@Query("url") url: String, @Query("ts") ts: Long?): JsonDict
}
