/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.widgets

import org.matrix.android.sdk.api.session.openid.OpenIdToken
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

internal interface WidgetsAPI {

    /**
     * Register to the server.
     *
     * @param body the body content (Ref: https://github.com/matrix-org/matrix-doc/pull/1961)
     * @param version the widget API version
     */
    @POST("register")
    suspend fun register(
            @Body body: OpenIdToken,
            @Query("v") version: String?
    ): RegisterWidgetResponse

    @GET("account")
    suspend fun validateToken(
            @Query("scalar_token") scalarToken: String?,
            @Query("v") version: String?
    )
}
