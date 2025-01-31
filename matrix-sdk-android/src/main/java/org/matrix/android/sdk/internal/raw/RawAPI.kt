/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.raw

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

internal interface RawAPI {
    @GET
    suspend fun getUrl(@Url url: String): ResponseBody
}
