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
import org.matrix.android.sdk.api.auth.data.SsoIdentityProvider

@JsonClass(generateAdapter = true)
internal data class LoginFlowResponse(
        /**
         * The homeserver's supported login types.
         */
        @Json(name = "flows")
        val flows: List<LoginFlow>?
)

@JsonClass(generateAdapter = true)
internal data class LoginFlow(
        /**
         * The login type. This is supplied as the type when logging in.
         */
        @Json(name = "type")
        val type: String?,

        /**
         * Augments m.login.sso flow discovery definition to include metadata on the supported IDPs
         * the client can show a button for each of the supported providers.
         * See MSC #2858
         */
        @Json(name = "identity_providers")
        val ssoIdentityProvider: List<SsoIdentityProvider>? = null

)
