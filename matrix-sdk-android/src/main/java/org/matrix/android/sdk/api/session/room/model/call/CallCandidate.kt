/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.call

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CallCandidate(
        /**
         * Required. The SDP media type this candidate is intended for.
         */
        @Json(name = "sdpMid") val sdpMid: String? = null,
        /**
         * Required. The index of the SDP 'm' line this candidate is intended for.
         */
        @Json(name = "sdpMLineIndex") val sdpMLineIndex: Int = 0,
        /**
         * Required. The SDP 'a' line of the candidate.
         */
        @Json(name = "candidate") val candidate: String? = null
)
