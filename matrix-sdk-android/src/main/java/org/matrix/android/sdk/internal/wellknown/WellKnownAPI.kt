/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.wellknown

import org.matrix.android.sdk.api.auth.data.WellKnown
import retrofit2.http.GET
import retrofit2.http.Path

internal interface WellKnownAPI {
    @GET("https://{domain}/.well-known/matrix/client")
    suspend fun getWellKnown(@Path("domain") domain: String): WellKnown
}
