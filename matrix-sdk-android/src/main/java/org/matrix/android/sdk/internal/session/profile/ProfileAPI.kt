/*
 * Copyright 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.matrix.android.sdk.internal.session.profile

import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface ProfileAPI {

    /**
     * Get the combined profile information for this user.
     * This API may be used to fetch the user's own profile information or other users; either locally or on remote homeservers.
     * This API may return keys which are not limited to displayname or avatar_url.
     * @param userId the user id to fetch profile info
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}")
    fun getProfile(@Path("userId") userId: String): Call<JsonDict>

    /**
     * List all 3PIDs linked to the Matrix user account.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid")
    fun getThreePIDs(): Call<AccountThreePidsResponse>

    /**
     * Change user display name
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}/displayname")
    fun setDisplayName(@Path("userId") userId: String,
                       @Body body: SetDisplayNameBody): Call<Unit>

    /**
     * Change user avatar url.
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}/avatar_url")
    fun setAvatarUrl(@Path("userId") userId: String,
                     @Body body: SetAvatarUrlBody): Call<Unit>

    /**
     * Bind a threePid
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-account-3pid-bind
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account/3pid/bind")
    fun bindThreePid(@Body body: BindThreePidBody): Call<Unit>

    /**
     * Unbind a threePid
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-account-3pid-unbind
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account/3pid/unbind")
    fun unbindThreePid(@Body body: UnbindThreePidBody): Call<UnbindThreePidResponse>
}
