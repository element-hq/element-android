/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

/**
 * Content of the event of type
 * [EventType.BEACON_LOCATION_DATA][org.matrix.android.sdk.api.session.events.model.EventType.BEACON_LOCATION_DATA]
 *
 * It contains location data related to a live location share.
 * It is related to the state event that originally started the live.
 * See [MessageBeaconInfoContent][org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent]
 */
@JsonClass(generateAdapter = true)
data class MessageBeaconLocationDataContent(
        /**
         * Local message type, not from server.
         */
        @Transient
        override val msgType: String = MessageType.MSGTYPE_BEACON_LOCATION_DATA,

        @Json(name = "body") override val body: String = "",
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,

        /**
         * See [MSC3488](https://github.com/matrix-org/matrix-doc/blob/matthew/location/proposals/3488-location.md).
         */
        @Json(name = "org.matrix.msc3488.location") val unstableLocationInfo: LocationInfo? = null,
        @Json(name = "m.location") val locationInfo: LocationInfo? = null,

        /**
         * Exact time that the data in the event refers to (milliseconds since the UNIX epoch).
         */
        @Json(name = "org.matrix.msc3488.ts") val unstableTimestampMillis: Long? = null,
        @Json(name = "m.ts") val timestampMillis: Long? = null
) : MessageContent {

    fun getBestLocationInfo() = locationInfo ?: unstableLocationInfo

    fun getBestTimestampMillis() = timestampMillis ?: unstableTimestampMillis
}
