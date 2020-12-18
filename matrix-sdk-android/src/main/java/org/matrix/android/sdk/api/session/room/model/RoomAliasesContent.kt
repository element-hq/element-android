/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing the EventType.STATE_ROOM_ALIASES state event content
 * Note that this Event has been deprecated, see
 * - https://matrix.org/docs/spec/client_server/r0.6.1#historical-events
 * - https://github.com/matrix-org/matrix-doc/pull/2432
 */
@JsonClass(generateAdapter = true)
data class RoomAliasesContent(
        @Json(name = "aliases") val aliases: List<String> = emptyList()
)
