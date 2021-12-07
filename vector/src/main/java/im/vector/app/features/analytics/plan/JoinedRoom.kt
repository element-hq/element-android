/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when the user joins a room.
 */
data class JoinedRoom(
    /**
     * Whether the room is a DM.
     */
    val isDM: Boolean,
    /**
     * The size of the room.
     */
    val roomSize: RoomSize,
) : VectorAnalyticsEvent {

    enum class RoomSize {
        ElevenToOneHundred,
        MoreThanAThousand,
        OneHundredAndOneToAThousand,
        ThreeToTen,
        Two,
    }

    override fun getName() = "JoinedRoom"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("isDM", isDM)
            put("roomSize", roomSize.name)
        }.takeIf { it.isNotEmpty() }
    }
}
