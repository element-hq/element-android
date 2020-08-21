/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.internal.auth.registration.SuccessResult
import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.identity.model.IdentityAccountResponse
import org.matrix.android.sdk.internal.session.identity.model.IdentityHashDetailResponse
import org.matrix.android.sdk.internal.session.identity.model.IdentityLookUpParams
import org.matrix.android.sdk.internal.session.identity.model.IdentityLookUpResponse
import org.matrix.android.sdk.internal.session.identity.model.IdentityRequestOwnershipParams
import org.matrix.android.sdk.internal.session.identity.model.IdentityRequestTokenForEmailBody
import org.matrix.android.sdk.internal.session.identity.model.IdentityRequestTokenForMsisdnBody
import org.matrix.android.sdk.internal.session.identity.model.IdentityRequestTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Ref: https://matrix.org/docs/spec/identity_service/latest
 * This contain the requests which need an identity server token
 */
internal interface IdentityAPI {
    /**
     * Gets information about what user owns the access token used in the request.
     * Will return a 403 for when terms are not signed
     * Ref: https://matrix.org/docs/spec/identity_service/latest#get-matrix-identity-v2-account
     */
    @GET(NetworkConstants.URI_IDENTITY_PATH_V2 + "account")
    fun getAccount(): Call<IdentityAccountResponse>

    /**
     * Logs out the access token, preventing it from being used to authenticate future requests to the server.
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "account/logout")
    fun logout(): Call<Unit>

    /**
     * Request the hash detail to request a bunch of 3PIDs
     * Ref: https://matrix.org/docs/spec/identity_service/latest#get-matrix-identity-v2-hash-details
     */
    @GET(NetworkConstants.URI_IDENTITY_PATH_V2 + "hash_details")
    fun hashDetails(): Call<IdentityHashDetailResponse>

    /**
     * Request a bunch of 3PIDs
     * Ref: https://matrix.org/docs/spec/identity_service/latest#post-matrix-identity-v2-lookup
     *
     * @param body the body request
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "lookup")
    fun lookup(@Body body: IdentityLookUpParams): Call<IdentityLookUpResponse>

    /**
     * Create a session to change the bind status of an email to an identity server
     * The identity server will also send an email
     *
     * @param body
     * @return the sid
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/email/requestToken")
    fun requestTokenToBindEmail(@Body body: IdentityRequestTokenForEmailBody): Call<IdentityRequestTokenResponse>

    /**
     * Create a session to change the bind status of an phone number to an identity server
     * The identity server will also send an SMS on the ThreePid provided
     *
     * @param body
     * @return the sid
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/msisdn/requestToken")
    fun requestTokenToBindMsisdn(@Body body: IdentityRequestTokenForMsisdnBody): Call<IdentityRequestTokenResponse>

    /**
     * Validate ownership of an email address, or a phone number.
     * Ref:
     * - https://matrix.org/docs/spec/identity_service/latest#post-matrix-identity-v2-validate-msisdn-submittoken
     * - https://matrix.org/docs/spec/identity_service/latest#post-matrix-identity-v2-validate-email-submittoken
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/{medium}/submitToken")
    fun submitToken(@Path("medium") medium: String,
                    @Body body: IdentityRequestOwnershipParams): Call<SuccessResult>
}
