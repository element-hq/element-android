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
internal data class DeleteThreePidResponse(
        /**
         * Required. An indicator as to whether or not the homeserver was able to unbind the 3PID from
         * the identity server. success indicates that the identity server has unbound the identifier
         * whereas no-support indicates that the identity server refuses to support the request or the
         * homeserver was not able to determine an identity server to unbind from. One of: ["no-support", "success"]
         */
        @Json(name = "id_server_unbind_result")
        val idServerUnbindResult: String? = null
)
