/*
 * Copyright (c) 2022 New Vector Ltd
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

package org.matrix.android.sdk.internal.auth

import org.matrix.android.sdk.internal.auth.data.RefreshParams
import org.matrix.android.sdk.internal.auth.data.RefreshResult
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * The refresh token REST API.
 */
internal interface RefreshTokenAPI {
    /**
     * Refresh the access token given a refresh token.
     * @param refreshParams the refresh parameters
     */
    @Headers("CONNECT_TIMEOUT:60000", "READ_TIMEOUT:60000", "WRITE_TIMEOUT:60000")
    @POST(NetworkConstants.URI_API_PREFIX_PATH_V1 + "refresh")
    suspend fun refreshToken(@Body refreshParams: RefreshParams): RefreshResult
}
