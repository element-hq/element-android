/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.pushers.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.MatrixPatterns

/**
 * In this case, the format is:
 * <pre>
 * {
 *     "event_id":"$anEventId",
 *     "room_id":"!aRoomId",
 *     "unread":"1",
 *     "prio":"high"
 * }
 * </pre>
 * .
 */
@JsonClass(generateAdapter = true)
data class PushDataFcm(
        @Json(name = "event_id") val eventId: String?,
        @Json(name = "room_id") val roomId: String?,
        @Json(name = "unread") var unread: Int?,
)

fun PushDataFcm.toPushData() = PushData(
        eventId = eventId?.takeIf { MatrixPatterns.isEventId(it) },
        roomId = roomId?.takeIf { MatrixPatterns.isRoomId(it) },
        unread = unread
)
