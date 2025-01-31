/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity

import org.matrix.android.sdk.api.session.identity.model.SignInvitationResult
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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Ref: https://matrix.org/docs/spec/identity_service/latest.
 * This contain the requests which need an identity server token.
 */
internal interface IdentityAPI {
    /**
     * Gets information about what user owns the access token used in the request.
     * Will return a 403 for when terms are not signed
     * Ref: https://matrix.org/docs/spec/identity_service/latest#get-matrix-identity-v2-account
     */
    @GET(NetworkConstants.URI_IDENTITY_PATH_V2 + "account")
    suspend fun getAccount(): IdentityAccountResponse

    /**
     * Logs out the access token, preventing it from being used to authenticate future requests to the server.
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "account/logout")
    suspend fun logout()

    /**
     * Request the hash detail to request a bunch of 3PIDs
     * Ref: https://matrix.org/docs/spec/identity_service/latest#get-matrix-identity-v2-hash-details
     */
    @GET(NetworkConstants.URI_IDENTITY_PATH_V2 + "hash_details")
    suspend fun hashDetails(): IdentityHashDetailResponse

    /**
     * Request a bunch of 3PIDs
     * Ref: https://matrix.org/docs/spec/identity_service/latest#post-matrix-identity-v2-lookup
     *
     * @param body the body request
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "lookup")
    suspend fun lookup(@Body body: IdentityLookUpParams): IdentityLookUpResponse

    /**
     * Create a session to change the bind status of an email to an identity server.
     * The identity server will also send an email.
     *
     * @param body
     * @return the sid
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/email/requestToken")
    suspend fun requestTokenToBindEmail(@Body body: IdentityRequestTokenForEmailBody): IdentityRequestTokenResponse

    /**
     * Create a session to change the bind status of an phone number to an identity server.
     * The identity server will also send an SMS on the ThreePid provided.
     *
     * @param body
     * @return the sid
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/msisdn/requestToken")
    suspend fun requestTokenToBindMsisdn(@Body body: IdentityRequestTokenForMsisdnBody): IdentityRequestTokenResponse

    /**
     * Validate ownership of an email address, or a phone number.
     * Ref:
     * - https://matrix.org/docs/spec/identity_service/latest#post-matrix-identity-v2-validate-msisdn-submittoken
     * - https://matrix.org/docs/spec/identity_service/latest#post-matrix-identity-v2-validate-email-submittoken
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/{medium}/submitToken")
    suspend fun submitToken(
            @Path("medium") medium: String,
            @Body body: IdentityRequestOwnershipParams
    ): SuccessResult

    /**
     * https://matrix.org/docs/spec/identity_service/r0.3.0#post-matrix-identity-v2-sign-ed25519.
     *
     * Have to rely on V1 for now.
     */
    @POST(NetworkConstants.URI_IDENTITY_PATH_V1 + "sign-ed25519")
    suspend fun signInvitationDetails(
            @Query("token") token: String,
            @Query("private_key") privateKey: String,
            @Query("mxid") mxid: String
    ): SignInvitationResult
}
