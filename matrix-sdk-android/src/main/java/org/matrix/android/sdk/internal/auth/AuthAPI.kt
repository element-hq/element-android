/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.internal.auth

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.internal.auth.data.LoginFlowResponse
import org.matrix.android.sdk.internal.auth.data.PasswordLoginParams
import org.matrix.android.sdk.internal.auth.data.RiotConfig
import org.matrix.android.sdk.internal.auth.data.TokenLoginParams
import org.matrix.android.sdk.internal.auth.login.ResetPasswordMailConfirmed
import org.matrix.android.sdk.internal.auth.registration.AddThreePidRegistrationParams
import org.matrix.android.sdk.internal.auth.registration.AddThreePidRegistrationResponse
import org.matrix.android.sdk.internal.auth.registration.RegistrationParams
import org.matrix.android.sdk.internal.auth.registration.SuccessResult
import org.matrix.android.sdk.internal.auth.registration.ValidationCodeBody
import org.matrix.android.sdk.internal.auth.version.Versions
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * The login REST API.
 */
internal interface AuthAPI {
    /**
     * Get a Riot config file, using the name including the domain
     */
    @GET("config.{domain}.json")
    fun getRiotConfigDomain(@Path("domain") domain: String): Call<RiotConfig>

    /**
     * Get a Riot config file
     */
    @GET("config.json")
    fun getRiotConfig(): Call<RiotConfig>

    /**
     * Get the version information of the homeserver
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_ + "versions")
    fun versions(): Call<Versions>

    /**
     * Register to the homeserver, or get error 401 with a RegistrationFlowResponse object if registration is incomplete
     * Ref: https://matrix.org/docs/spec/client_server/latest#account-registration-and-management
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "register")
    fun register(@Body registrationParams: RegistrationParams): Call<Credentials>

    /**
     * Add 3Pid during registration
     * Ref: https://gist.github.com/jryans/839a09bf0c5a70e2f36ed990d50ed928
     * https://github.com/matrix-org/matrix-doc/pull/2290
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "register/{threePid}/requestToken")
    fun add3Pid(@Path("threePid") threePid: String,
                @Body params: AddThreePidRegistrationParams): Call<AddThreePidRegistrationResponse>

    /**
     * Validate 3pid
     */
    @POST
    fun validate3Pid(@Url url: String,
                     @Body params: ValidationCodeBody): Call<SuccessResult>

    /**
     * Get the supported login flow
     * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-login
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    fun getLoginFlows(): Call<LoginFlowResponse>

    /**
     * Pass params to the server for the current login phase.
     * Set all the timeouts to 1 minute
     *
     * @param loginParams the login parameters
     */
    @Headers("CONNECT_TIMEOUT:60000", "READ_TIMEOUT:60000", "WRITE_TIMEOUT:60000")
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    fun login(@Body loginParams: PasswordLoginParams): Call<Credentials>

    // Unfortunately we cannot use interface for @Body parameter, so I duplicate the method for the type TokenLoginParams
    @Headers("CONNECT_TIMEOUT:60000", "READ_TIMEOUT:60000", "WRITE_TIMEOUT:60000")
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login")
    fun login(@Body loginParams: TokenLoginParams): Call<Credentials>

    /**
     * Ask the homeserver to reset the password associated with the provided email.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password/email/requestToken")
    fun resetPassword(@Body params: AddThreePidRegistrationParams): Call<AddThreePidRegistrationResponse>

    /**
     * Ask the homeserver to reset the password with the provided new password once the email is validated.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password")
    fun resetPasswordMailConfirmed(@Body params: ResetPasswordMailConfirmed): Call<Unit>
}
