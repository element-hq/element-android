/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.MatrixPatterns

/**
 * In this case, the format is:
 * <pre>
 * {
 *     "notification":{
 *         "event_id":"$anEventId",
 *         "room_id":"!aRoomId",
 *         "counts":{
 *             "unread":1
 *         },
 *         "prio":"high"
 *     }
 * }
 * </pre>
 * .
 */
@JsonClass(generateAdapter = true)
data class PushDataUnifiedPush(
        @Json(name = "notification") val notification: PushDataUnifiedPushNotification?
)

@JsonClass(generateAdapter = true)
data class PushDataUnifiedPushNotification(
        @Json(name = "event_id") val eventId: String?,
        @Json(name = "room_id") val roomId: String?,
        @Json(name = "counts") var counts: PushDataUnifiedPushCounts?,
)

@JsonClass(generateAdapter = true)
data class PushDataUnifiedPushCounts(
        @Json(name = "unread") val unread: Int?
)

fun PushDataUnifiedPush.toPushData() = PushData(
        eventId = notification?.eventId?.takeIf { MatrixPatterns.isEventId(it) },
        roomId = notification?.roomId?.takeIf { MatrixPatterns.isRoomId(it) },
        unread = notification?.counts?.unread
)
