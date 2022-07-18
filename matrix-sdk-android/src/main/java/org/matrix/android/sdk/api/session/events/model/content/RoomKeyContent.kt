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
package org.matrix.android.sdk.api.session.events.model.content

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an sharekey content.
 */
@JsonClass(generateAdapter = true)
data class RoomKeyContent(

        @Json(name = "algorithm")
        val algorithm: String? = null,

        @Json(name = "room_id")
        val roomId: String? = null,

        @Json(name = "session_id")
        val sessionId: String? = null,

        @Json(name = "session_key")
        val sessionKey: String? = null,

        // should be a Long but it is sometimes a double
        @Json(name = "chain_index")
        val chainIndex: Any? = null,

        /**
         * MSC3061 Identifies keys that were sent when the room's visibility setting was set to world_readable or shared.
         */
        @Json(name = "org.matrix.msc3061.shared_history")
        val sharedHistory: Boolean? = false

)
