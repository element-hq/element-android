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
 * Content of the state event of type
 * [EventType.STATE_ROOM_BEACON_INFO][org.matrix.android.sdk.api.session.events.model.EventType.STATE_ROOM_BEACON_INFO]
 *
 * It contains general info related to a live location share.
 * Locations are sent in a different message related to the state event.
 * See [MessageBeaconLocationDataContent][org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent]
 */
@JsonClass(generateAdapter = true)
data class MessageBeaconInfoContent(
        /**
         * Local message type, not from server.
         */
        @Transient
        override val msgType: String = MessageType.MSGTYPE_BEACON_INFO,

        @Json(name = "body") override val body: String = "",
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,

        /**
         * Optional description of the beacon.
         */
        @Json(name = "description") val description: String? = null,
        /**
         * Beacon should be considered as inactive after this timeout as milliseconds.
         */
        @Json(name = "timeout") val timeout: Long? = null,
        /**
         * Should be set true to start sharing beacon.
         */
        @Json(name = "live") val isLive: Boolean? = null,

        /**
         * Beacon creation timestamp.
         */
        @Json(name = "org.matrix.msc3488.ts") val unstableTimestampMillis: Long? = null,
        @Json(name = "m.ts") val timestampMillis: Long? = null,
        /**
         * Live location asset type.
         */
        @Json(name = "org.matrix.msc3488.asset") val unstableLocationAsset: LocationAsset = LocationAsset(LocationAssetType.SELF),
        @Json(name = "m.asset") val locationAsset: LocationAsset? = null,
) : MessageContent {

    fun getBestTimestampMillis() = timestampMillis ?: unstableTimestampMillis

    fun getBestLocationAsset() = locationAsset ?: unstableLocationAsset
}
