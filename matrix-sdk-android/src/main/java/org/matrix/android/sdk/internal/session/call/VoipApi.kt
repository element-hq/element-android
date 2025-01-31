/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.call

import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.GET

internal interface VoipApi {

    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "voip/turnServer")
    suspend fun getTurnServer(): TurnServerResponse
}
