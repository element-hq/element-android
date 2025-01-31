/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.terms

import org.matrix.android.sdk.api.session.terms.TermsResponse
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.emptyJsonDict
import org.matrix.android.sdk.internal.network.HttpHeaders
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

internal interface TermsAPI {
    /**
     * This request does not require authentication.
     */
    @GET
    suspend fun getTerms(@Url url: String): TermsResponse

    /**
     * This request requires authentication.
     */
    @POST
    suspend fun agreeToTerms(
            @Url url: String,
            @Body params: AcceptTermsBody,
            @Header(HttpHeaders.Authorization) token: String
    )

    /**
     * API to retrieve the terms for a homeserver. The API /terms does not exist yet, so retrieve the terms from the login flow.
     * We do not care about the result (Credentials).
     */
    @POST
    suspend fun register(
            @Url url: String,
            @Body body: JsonDict = emptyJsonDict
    )
}
