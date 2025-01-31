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
data class ThirdPartyProtocol(
        /**
         * Required. Fields which may be used to identify a third party user. These should be ordered to suggest the way that entities may be grouped,
         * where higher groupings are ordered first. For example, the name of a network should be searched before the nickname of a user.
         */
        @Json(name = "user_fields")
        val userFields: List<String>? = null,

        /**
         * Required. Fields which may be used to identify a third party location. These should be ordered to suggest the way that
         * entities may be grouped, where higher groupings are ordered first. For example, the name of a network should be
         * searched before the name of a channel.
         */
        @Json(name = "location_fields")
        val locationFields: List<String>? = null,

        /**
         * Required. A content URI representing an icon for the third party protocol.
         *
         * FIXDOC: This field was not present in legacy Riot, and it is sometimes sent by the server (so not Required?)
         */
        @Json(name = "icon")
        val icon: String? = null,

        /**
         * Required. The type definitions for the fields defined in the user_fields and location_fields. Each entry in those arrays MUST have an entry here.
         * The string key for this object is field name itself.
         *
         * May be an empty object if no fields are defined.
         */
        @Json(name = "field_types")
        val fieldTypes: Map<String, FieldType>? = null,

        /**
         * Required. A list of objects representing independent instances of configuration. For example, multiple networks on IRC
         * if multiple are provided by the same application service.
         */
        @Json(name = "instances")
        val instances: List<ThirdPartyProtocolInstance>? = null
)
