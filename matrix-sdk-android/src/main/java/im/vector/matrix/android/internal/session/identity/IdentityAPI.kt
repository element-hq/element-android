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

import im.vector.matrix.android.internal.auth.registration.SuccessResult
import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.identity.model.IdentityAccountResponse
import im.vector.matrix.android.internal.session.identity.model.IdentityHashDetailResponse
import im.vector.matrix.android.internal.session.identity.model.IdentityLookUpV2Params
import im.vector.matrix.android.internal.session.identity.model.IdentityLookUpV2Response
import im.vector.matrix.android.internal.session.identity.model.IdentityRequestOwnershipParams
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
     */
    @GET(NetworkConstants.URI_IDENTITY_PATH_V2 + "account")
    fun getAccount(): Call<IdentityAccountResponse>

    /**
     * Logs out the access token, preventing it from being used to authenticate future requests to the server.
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "logout")
    fun logout(): Call<Unit>

    /**
     * Request the hash detail to request a bunch of 3PIDs
     */
    @GET(NetworkConstants.URI_IDENTITY_PATH_V2 + "hash_details")
    fun hashDetails(): Call<IdentityHashDetailResponse>

    /**
     * Request a bunch of 3PIDs
     *
     * @param body the body request
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "lookup")
    fun bulkLookupV2(@Body body: IdentityLookUpV2Params): Call<IdentityLookUpV2Response>

    /**
     * Request the ownership validation of an email address or a phone number previously set
     * by [ProfileApi.requestEmailValidation]
     *
     * @param medium the medium of the 3pid
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/{medium}/submitToken")
    fun requestOwnershipValidationV2(@Path("medium") medium: String?,
                                     @Body body: IdentityRequestOwnershipParams): Call<SuccessResult>
}
