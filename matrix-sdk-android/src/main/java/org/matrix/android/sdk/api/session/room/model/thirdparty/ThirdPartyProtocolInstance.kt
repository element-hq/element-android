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
data class ThirdPartyProtocolInstance(
        /**
         * Required. A human-readable description for the protocol, such as the name.
         */
        @Json(name = "desc")
        val desc: String? = null,

        /**
         * An optional content URI representing the protocol. Overrides the one provided at the higher level Protocol object.
         */
        @Json(name = "icon")
        val icon: String? = null,

        /**
         * Required. Preset values for fields the client may use to search by.
         */
        @Json(name = "fields")
        val fields: Map<String, Any>? = null,

        /**
         * Required. A unique identifier across all instances.
         */
        @Json(name = "network_id")
        val networkId: String? = null,

        /**
         * FIXDOC Not documented on matrix.org doc.
         */
        @Json(name = "instance_id")
        val instanceId: String? = null,

        /**
         * FIXDOC Not documented on matrix.org doc.
         */
        @Json(name = "bot_user_id")
        val botUserId: String? = null
)
