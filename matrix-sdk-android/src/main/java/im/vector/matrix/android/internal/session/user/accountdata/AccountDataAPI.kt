/*
 * Copyright 2019 New Vector Ltd
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
 */

package im.vector.matrix.android.internal.session.user.accountdata

import im.vector.matrix.android.internal.network.NetworkConstants
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AccountDataAPI {

    /**
     * Set some account_data for the client.
     *
     * @param userId the user id
     * @param type   the type
     * @param params the put params
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/account_data/{type}")
    fun setAccountData(@Path("userId") userId: String, @Path("type") type: String, @Body params: Any): Call<Unit>

    /**
     * Gets a bearer token from the homeserver that the user can
     * present to a third party in order to prove their ownership
     * of the Matrix account they are logged into.
     *
     * @param userId the user id
     * @param body   the body content
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/openid/request_token")
    fun openIdToken(@Path("userId") userId: String, @Body body: Map<Any, Any>): Call<Map<Any, Any>>
}
