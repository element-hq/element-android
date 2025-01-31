/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

@JsonClass(generateAdapter = true)
internal data class TokenLoginParams(
        @Json(name = "type") override val type: String = LoginFlowTypes.TOKEN,
        @Json(name = "token") val token: String
) : LoginParams
