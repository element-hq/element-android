/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model.roomdirectory

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class to pass parameters to get the public rooms list.
 */
@JsonClass(generateAdapter = true)
data class PublicRoomsParams(
        /**
         * Limit the number of results returned.
         */
        @Json(name = "limit")
        val limit: Int? = null,

        /**
         * A pagination token from a previous request, allowing clients to get the next (or previous) batch of rooms.
         * The direction of pagination is specified solely by which token is supplied, rather than via an explicit flag.
         */
        @Json(name = "since")
        val since: String? = null,

        /**
         * Filter to apply to the results.
         */
        @Json(name = "filter")
        val filter: PublicRoomsFilter? = null,

        /**
         * Whether or not to include all known networks/protocols from application services on the homeserver. Defaults to false.
         */
        @Json(name = "include_all_networks")
        val includeAllNetworks: Boolean = false,

        /**
         * The specific third party network/protocol to request from the homeserver. Can only be used if include_all_networks is false.
         */
        @Json(name = "third_party_instance_id")
        val thirdPartyInstanceId: String? = null
)
