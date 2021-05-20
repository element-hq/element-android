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
 */

package org.matrix.android.sdk.internal.session.thirdparty

import org.matrix.android.sdk.api.session.room.model.thirdparty.OpenIdToken
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import org.matrix.android.sdk.api.session.thirdparty.model.ThirdPartyUser
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

internal interface ThirdPartyAPI {

    /**
     * Get the third party server protocols.
     *
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1.html#get-matrix-client-r0-thirdparty-protocols
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "thirdparty/protocols")
    suspend fun thirdPartyProtocols(): Map<String, ThirdPartyProtocol>

    /**
     * Retrieve a Matrix User ID linked to a user on the third party service, given a set of user parameters.
     *
     * Ref: https://matrix.org/docs/spec/client_server/r0.6.1#get-matrix-client-r0-thirdparty-user-protocol
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "thirdparty/protocols/user/{protocol}")
    suspend fun getThirdPartyUser(@Path("protocol") protocol: String,
                                  @QueryMap params: Map<String, String>?): List<ThirdPartyUser>

    /**
     * Gets an OpenID token object that the requester may supply to another service to verify their identity in Matrix.
     * The generated token is only valid for exchanging for user information from the federation API for OpenID.
     * The access token generated is only valid for the OpenID API. It cannot be used to request another OpenID access token or call /sync, for example.
     *
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-user-userid-openid-request-token
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/openid/request_token")
    suspend fun requestOpenIdToken(@Path("userId") userId: String,
                                   // We should post an empty body
                                   @Body body: JsonDict = HashMap()): OpenIdToken


}
