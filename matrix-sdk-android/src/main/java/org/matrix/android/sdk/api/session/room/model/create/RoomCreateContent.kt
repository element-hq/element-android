/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.model.create

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Content of a m.room.create type event
 */
@JsonClass(generateAdapter = true)
data class RoomCreateContent(
        @Json(name = "creator") val creator: String? = null,
        @Json(name = "room_version") val roomVersion: String? = null,
        @Json(name = "predecessor") val predecessor: Predecessor? = null,
        // Defines the room type, see #RoomType (user extensible)
        @Json(name = "type") val type: String? = null
)
