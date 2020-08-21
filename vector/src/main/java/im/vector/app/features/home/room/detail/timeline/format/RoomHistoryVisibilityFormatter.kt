/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.format

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

class RoomHistoryVisibilityFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {

    fun format(roomHistoryVisibility: RoomHistoryVisibility): String {
        return when (roomHistoryVisibility) {
            RoomHistoryVisibility.SHARED         -> stringProvider.getString(R.string.notice_room_visibility_shared)
            RoomHistoryVisibility.INVITED        -> stringProvider.getString(R.string.notice_room_visibility_invited)
            RoomHistoryVisibility.JOINED         -> stringProvider.getString(R.string.notice_room_visibility_joined)
            RoomHistoryVisibility.WORLD_READABLE -> stringProvider.getString(R.string.notice_room_visibility_world_readable)
        }
    }
}
