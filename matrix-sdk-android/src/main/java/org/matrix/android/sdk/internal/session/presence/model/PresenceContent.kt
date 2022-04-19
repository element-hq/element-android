/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.presence.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.presence.model.PresenceEnum

/**
 * Class representing the EventType.PRESENCE event content
 */
@JsonClass(generateAdapter = true)
internal data class PresenceContent(
        /**
         * Required. The presence state for this user. One of: ["online", "offline", "unavailable"]
         */
        @Json(name = "presence") val presence: PresenceEnum,
        /**
         * The last time since this used performed some action, in milliseconds.
         */
        @Json(name = "last_active_ago") val lastActiveAgo: Long? = null,
        /**
         * An optional description to accompany the presence.
         */
        @Json(name = "status_msg") val statusMessage: String? = null,
        /**
         * Whether the user is currently active
         */
        @Json(name = "currently_active") val isCurrentlyActive: Boolean = false,
        /**
         * The current avatar URL for this user, if any.
         */
        @Json(name = "avatar_url") val avatarUrl: String? = null,
        /**
         * The current display name for this user, if any.
         */
        @Json(name = "displayname") val displayName: String? = null
)
