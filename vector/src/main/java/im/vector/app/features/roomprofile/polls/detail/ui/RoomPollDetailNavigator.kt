/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import android.content.Context
import im.vector.app.features.navigation.Navigator
import javax.inject.Inject

class RoomPollDetailNavigator @Inject constructor(
        private val navigator: Navigator,
) {

    fun goToTimelineEvent(context: Context, roomId: String, eventId: String) {
        navigator.openRoom(
                context = context,
                roomId = roomId,
                eventId = eventId,
                buildTask = true,
        )
    }
}
