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

import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

class RoomHistoryVisibilityFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun getNoticeSuffix(roomHistoryVisibility: RoomHistoryVisibility): String {
        return stringProvider.getString(
                when (roomHistoryVisibility) {
                    RoomHistoryVisibility.WORLD_READABLE -> CommonStrings.notice_room_visibility_world_readable
                    RoomHistoryVisibility.SHARED -> CommonStrings.notice_room_visibility_shared
                    RoomHistoryVisibility.INVITED -> CommonStrings.notice_room_visibility_invited
                    RoomHistoryVisibility.JOINED -> CommonStrings.notice_room_visibility_joined
                }
        )
    }

    fun getSetting(roomHistoryVisibility: RoomHistoryVisibility): String {
        return stringProvider.getString(
                when (roomHistoryVisibility) {
                    RoomHistoryVisibility.WORLD_READABLE -> CommonStrings.room_settings_read_history_entry_anyone
                    RoomHistoryVisibility.SHARED -> CommonStrings.room_settings_read_history_entry_members_only_option_time_shared
                    RoomHistoryVisibility.INVITED -> CommonStrings.room_settings_read_history_entry_members_only_invited
                    RoomHistoryVisibility.JOINED -> CommonStrings.room_settings_read_history_entry_members_only_joined
                }
        )
    }
}
