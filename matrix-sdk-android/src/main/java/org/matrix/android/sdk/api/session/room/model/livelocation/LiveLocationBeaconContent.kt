/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.room.model.livelocation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.message.LocationAsset
import org.matrix.android.sdk.api.session.room.model.message.LocationAssetType
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageLiveLocationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

@JsonClass(generateAdapter = true)
data class LiveLocationBeaconContent(
        /**
         * Local message type, not from server
         */
        @Transient
        override val msgType: String = MessageType.MSGTYPE_LIVE_LOCATION_STATE,

        @Json(name = "body") override val body: String = "",
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,

        /**
         * Indicates user's intent to share ephemeral location.
         */
        @Json(name = "org.matrix.msc3672.beacon_info") val unstableBeaconInfo: BeaconInfo? = null,
        @Json(name = "m.beacon_info") val beaconInfo: BeaconInfo? = null,
        /**
         * Beacon creation timestamp.
         */
        @Json(name = "org.matrix.msc3488.ts") val unstableTimestampAsMilliseconds: Long? = null,
        @Json(name = "m.ts") val timestampAsMilliseconds: Long? = null,
        /**
         * Live location asset type.
         */
        @Json(name = "org.matrix.msc3488.asset") val unstableLocationAsset: LocationAsset = LocationAsset(LocationAssetType.SELF),
        @Json(name = "m.asset") val locationAsset: LocationAsset? = null,

        /**
         * Client side tracking of the last location
         */
        var lastLocationContent: MessageLiveLocationContent? = null,

        /**
         * Client side tracking of whether the beacon has timed out.
         */
        var hasTimedOut: Boolean = false
) : MessageContent {

    fun getBestBeaconInfo() = beaconInfo ?: unstableBeaconInfo

    fun getBestTimestampAsMilliseconds() = timestampAsMilliseconds ?: unstableTimestampAsMilliseconds

    fun getBestLocationAsset() = locationAsset ?: unstableLocationAsset
}
