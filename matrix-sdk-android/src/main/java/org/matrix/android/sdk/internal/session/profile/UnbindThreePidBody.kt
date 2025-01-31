/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UnbindThreePidBody(
        /**
         * The identity server to unbind from. If not provided, the homeserver MUST use the id_server the identifier was added through.
         * If the homeserver does not know the original id_server, it MUST return a id_server_unbind_result of no-support.
         */
        @Json(name = "id_server")
        val identityServerUrlWithoutProtocol: String?,

        /**
         * Required. The medium of the third party identifier being removed. One of: ["email", "msisdn"]
         */
        @Json(name = "medium")
        val medium: String,

        /**
         * Required. The third party address being removed.
         */
        @Json(name = "address")
        val address: String
)
