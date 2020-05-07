/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.profile

import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.network.NetworkConstants
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ProfileAPI {

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
}
