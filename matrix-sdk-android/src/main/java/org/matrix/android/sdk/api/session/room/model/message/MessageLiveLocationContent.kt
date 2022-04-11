/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

@JsonClass(generateAdapter = true)
data class MessageLiveLocationContent(
        /**
         * Local message type, not from server
         */
        @Transient
        override val msgType: String = MessageType.MSGTYPE_LIVE_LOCATION,

        @Json(name = "body") override val body: String = "",
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,

        /**
         * See [MSC3488](https://github.com/matrix-org/matrix-doc/blob/matthew/location/proposals/3488-location.md)
         */
        @Json(name = "org.matrix.msc3488.location") val unstableLocationInfo: LocationInfo? = null,
        @Json(name = "m.location") val locationInfo: LocationInfo? = null,

        /**
         * Exact time that the data in the event refers to (milliseconds since the UNIX epoch)
         */
        @Json(name = "org.matrix.msc3488.ts") val unstableTimestampAsMilliseconds: Long? = null,
        @Json(name = "m.ts") val timestampAsMilliseconds: Long? = null
) : MessageContent {

    fun getBestLocationInfo() = locationInfo ?: unstableLocationInfo

    fun getBestTimestampAsMilliseconds() = timestampAsMilliseconds ?: unstableTimestampAsMilliseconds
}
