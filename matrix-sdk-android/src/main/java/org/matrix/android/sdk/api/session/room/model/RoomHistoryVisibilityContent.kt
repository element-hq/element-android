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

package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class RoomHistoryVisibilityContent(
        @Json(name = "history_visibility") val _historyVisibility: String? = null
) {
    val historyVisibility: RoomHistoryVisibility? = when (_historyVisibility) {
        "world_readable" -> RoomHistoryVisibility.WORLD_READABLE
        "shared"         -> RoomHistoryVisibility.SHARED
        "invited"        -> RoomHistoryVisibility.INVITED
        "joined"         -> RoomHistoryVisibility.JOINED
        else             -> {
            Timber.w("Invalid value for RoomHistoryVisibility: `$_historyVisibility`")
            null
        }
    }
}
