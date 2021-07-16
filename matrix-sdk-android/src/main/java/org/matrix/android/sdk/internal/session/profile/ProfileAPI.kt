/*
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
import org.matrix.android.sdk.internal.auth.registration.SuccessResult
import org.matrix.android.sdk.internal.auth.registration.ValidationCodeBody
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

internal interface ProfileAPI {
    /**
     * Get the combined profile information for this user.
     * This API may be used to fetch the user's own profile information or other users; either locally or on remote homeservers.
     * This API may return keys which are not limited to displayname or avatar_url.
     * If server is configured as limit_profile_requests_to_users_who_share_rooms: true then response can be HTTP 403.
     * @param userId the user id to fetch profile info
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): JsonDict

    /**
     * List all 3PIDs linked to the Matrix user account.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid")
    suspend fun getThreePIDs(): AccountThreePidsResponse

    /**
     * Change user display name
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}/displayname")
    suspend fun setDisplayName(@Path("userId") userId: String,
                               @Body body: SetDisplayNameBody)

    /**
     * Change user avatar url.
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}/avatar_url")
    suspend fun setAvatarUrl(@Path("userId") userId: String,
                             @Body body: SetAvatarUrlBody)

    /**
     * Bind a threePid
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-account-3pid-bind
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account/3pid/bind")
    suspend fun bindThreePid(@Body body: BindThreePidBody)

    /**
     * Unbind a threePid
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-account-3pid-unbind
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account/3pid/unbind")
    suspend fun unbindThreePid(@Body body: UnbindThreePidBody): UnbindThreePidResponse

    /**
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#post-matrix-client-r0-account-3pid-email-requesttoken
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/email/requestToken")
    suspend fun addEmail(@Body body: AddEmailBody): AddEmailResponse

    /**
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#post-matrix-client-r0-account-3pid-msisdn-requesttoken
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/msisdn/requestToken")
    suspend fun addMsisdn(@Body body: AddMsisdnBody): AddMsisdnResponse

    /**
     * Validate Msisdn code (same model than for identity server API)
     */
    @POST
    suspend fun validateMsisdn(@Url url: String,
                               @Body params: ValidationCodeBody): SuccessResult

    /**
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#post-matrix-client-r0-account-3pid-add
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/add")
    suspend fun finalizeAddThreePid(@Body body: FinalizeAddThreePidBody)

    /**
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#post-matrix-client-r0-account-3pid-delete
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/delete")
    suspend fun deleteThreePid(@Body body: DeleteThreePidBody): DeleteThreePidResponse
}
