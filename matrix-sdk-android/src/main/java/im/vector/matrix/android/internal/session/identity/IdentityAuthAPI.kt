/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.identity

import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.identity.model.IdentityRegisterResponse
import im.vector.matrix.android.internal.session.openid.RequestOpenIdTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Ref: https://matrix.org/docs/spec/identity_service/latest
 * This contain the requests which do not need an identity server token
 */
internal interface IdentityAuthAPI {

    /**
     * https://matrix.org/docs/spec/client_server/r0.4.0.html#server-discovery
     * Simple ping call to check if server alive
     *
     * Ref: https://matrix.org/docs/spec/identity_service/unstable#status-check
     *
     * @return 200 in case of success
     */
    @GET(NetworkConstants.URI_API_PREFIX_IDENTITY)
    fun ping(): Call<Void>

    /**
     * Exchanges an OpenID token from the homeserver for an access token to access the identity server.
     * The request body is the same as the values returned by /openid/request_token in the Client-Server API.
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "account/register")
    fun register(@Body openIdToken: RequestOpenIdTokenResponse): Call<IdentityRegisterResponse>
}
