/*
 * Copyright 2024 The Matrix.org Foundation C.I.C.
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

@JsonClass(generateAdapter = true)
data class ElementCallNotifyContent(
        @Json(name = "application") val application: String? = null,
        @Json(name = "call_id") val callId: String? = null,
        @Json(name = "m.mentions") val mentions: Mentions? = null,
        @Json(name = "notify_type") val notifyType: String? = null,
)

@JsonClass(generateAdapter = true)
data class Mentions(
        @Json(name = "room") val room: Boolean? = null,
        @Json(name = "user_ids") val userIds: List<String>? = null,
)

fun ElementCallNotifyContent.isUserMentioned(userId: String): Boolean {
    return mentions?.room == true ||
            mentions?.userIds?.contains(userId) == true
}
