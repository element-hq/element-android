/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.thirdparty

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FieldType(
        /**
         * Required. A regular expression for validation of a field's value. This may be relatively coarse to verify the value as the application
         * service providing this protocol may apply additional
         */
        @Json(name = "regexp")
        val regexp: String? = null,

        /**
         * Required. An placeholder serving as a valid example of the field value.
         */
        @Json(name = "placeholder")
        val placeholder: String? = null
)
