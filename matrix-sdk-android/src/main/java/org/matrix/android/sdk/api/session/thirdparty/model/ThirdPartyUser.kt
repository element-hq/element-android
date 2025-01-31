/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.thirdparty.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.util.JsonDict

@JsonClass(generateAdapter = true)
data class ThirdPartyUser(
        /**
         * Required. A Matrix User ID representing a third party user.
         */
        @Json(name = "userid") val userId: String,
        /**
         * Required. The protocol ID that the third party location is a part of.
         */
        @Json(name = "protocol") val protocol: String,
        /**
         *  Required. Information used to identify this third party location.
         */
        @Json(name = "fields") val fields: JsonDict
)
