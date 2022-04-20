/*
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
     * This request does not require authentication
     */
    @GET
    suspend fun getTerms(@Url url: String): TermsResponse

    /**
     * This request requires authentication
     */
    @POST
    suspend fun agreeToTerms(@Url url: String,
                             @Body params: AcceptTermsBody,
                             @Header(HttpHeaders.Authorization) token: String)

    /**
     * API to retrieve the terms for a homeserver. The API /terms does not exist yet, so retrieve the terms from the login flow.
     * We do not care about the result (Credentials)
     */
    @POST
    suspend fun register(@Url url: String,
                         @Body body: JsonDict = emptyJsonDict)
}
