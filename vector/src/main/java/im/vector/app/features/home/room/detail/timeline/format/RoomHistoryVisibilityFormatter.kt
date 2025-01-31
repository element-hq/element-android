/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.format

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

class RoomHistoryVisibilityFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun getNoticeSuffix(roomHistoryVisibility: RoomHistoryVisibility): String {
        return stringProvider.getString(
                when (roomHistoryVisibility) {
                    RoomHistoryVisibility.WORLD_READABLE -> R.string.notice_room_visibility_world_readable
                    RoomHistoryVisibility.SHARED -> R.string.notice_room_visibility_shared
                    RoomHistoryVisibility.INVITED -> R.string.notice_room_visibility_invited
                    RoomHistoryVisibility.JOINED -> R.string.notice_room_visibility_joined
                }
        )
    }

    fun getSetting(roomHistoryVisibility: RoomHistoryVisibility): String {
        return stringProvider.getString(
                when (roomHistoryVisibility) {
                    RoomHistoryVisibility.WORLD_READABLE -> R.string.room_settings_read_history_entry_anyone
                    RoomHistoryVisibility.SHARED -> R.string.room_settings_read_history_entry_members_only_option_time_shared
                    RoomHistoryVisibility.INVITED -> R.string.room_settings_read_history_entry_members_only_invited
                    RoomHistoryVisibility.JOINED -> R.string.room_settings_read_history_entry_members_only_joined
                }
        )
    }
}
