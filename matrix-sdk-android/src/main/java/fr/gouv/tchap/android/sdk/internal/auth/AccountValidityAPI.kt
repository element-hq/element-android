/*
 * Copyright (c) 2021 New Vector Ltd
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
package fr.gouv.tchap.android.sdk.internal.auth

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AccountValidityAPI {
    /**
     * Trigger sending a renewal email to the user that made the request.
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account_validity/send_mail")
    suspend fun requestRenewalEmail()

    /**
     * Submit a token to renew the account validity.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account_validity/renew")
    suspend fun renewAccountValidity(@Query("token") token: String?)
}
