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

package org.matrix.client.api

import org.matrix.client.model.AddThreePidRegistrationParams
import org.matrix.client.model.AddThreePidRegistrationResponse
import org.matrix.client.model.Availability
import org.matrix.client.model.Credentials
import org.matrix.client.model.LoginFlowResponse
import org.matrix.client.model.PasswordLoginParams
import org.matrix.client.model.RegistrationParams
import org.matrix.client.model.ResetPasswordMailConfirmed
import org.matrix.client.model.SuccessResult
import org.matrix.client.model.TokenLoginParams
import org.matrix.client.model.ValidationCodeBody
import org.matrix.client.model.WebClientConfig
import org.matrix.client.utils.JsonDict
import org.matrix.client.utils.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * The login REST API.
 */
interface AuthAPI {
    /**
     * Get a Web client config file, using the name including the domain
     */
    @GET("config.{domain}.json")
    suspend fun getWebClientConfigDomain(@Path("domain") domain: String): WebClientConfig

    /**
     * Get a Web client default config file
     */
    @GET("config.json")
    suspend fun getWebClientConfig(): WebClientConfig

    /**
     * Register to the homeserver, or get error 401 with a RegistrationFlowResponse object if registration is incomplete
     * Ref: https://matrix.org/docs/spec/client_server/latest#account-registration-and-management
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "register")
    suspend fun register(@Body registrationParams: RegistrationParams): Credentials

    /**
     * Checks to see if a username is available, and valid, for the server.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "register/available")
    suspend fun registerAvailable(@Query("username") username: String): Availability

    /**
     * Get the combined profile information for this user.
     * This API may be used to fetch the user's own profile information or other users; either locally or on remote homeservers.
     * This API may return keys which are not limited to displayname or avatar_url.
     * @param userId the user id to fetch profile info
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): JsonDict

    /**
     * Add 3Pid during registration
     * Ref: https://gist.github.com/jryans/839a09bf0c5a70e2f36ed990d50ed928
     * https://github.com/matrix-org/matrix-doc/pull/2290
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "register/{threePid}/requestToken")
    suspend fun add3Pid(@Path("threePid") threePid: String,
                        @Body params: AddThreePidRegistrationParams): AddThreePidRegistrationResponse

    /**
     * Validate 3pid
     */
    @POST
    suspend fun validate3Pid(@Url url: String,
                             @Body params: ValidationCodeBody): SuccessResult

    /**
     * Get the supported login flow
     * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-login
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    suspend fun getLoginFlows(): LoginFlowResponse

    /**
     * Pass params to the server for the current login phase.
     * Set all the timeouts to 1 minute
     *
     * @param loginParams the login parameters
     */
    @Headers("CONNECT_TIMEOUT:60000", "READ_TIMEOUT:60000", "WRITE_TIMEOUT:60000")
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    suspend fun login(@Body loginParams: PasswordLoginParams): Credentials

    // Unfortunately we cannot use interface for @Body parameter, so I duplicate the method for the type TokenLoginParams
    @Headers("CONNECT_TIMEOUT:60000", "READ_TIMEOUT:60000", "WRITE_TIMEOUT:60000")
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    suspend fun login(@Body loginParams: TokenLoginParams): Credentials

    /**
     * Ask the homeserver to reset the password associated with the provided email.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password/email/requestToken")
    suspend fun resetPassword(@Body params: AddThreePidRegistrationParams): AddThreePidRegistrationResponse

    /**
     * Ask the homeserver to reset the password with the provided new password once the email is validated.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password")
    suspend fun resetPasswordMailConfirmed(@Body params: ResetPasswordMailConfirmed)
}
