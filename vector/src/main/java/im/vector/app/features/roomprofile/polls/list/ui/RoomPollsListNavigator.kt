/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.ui

import android.content.Context
import im.vector.app.features.roomprofile.polls.detail.ui.RoomPollDetailActivity
import javax.inject.Inject

class RoomPollsListNavigator @Inject constructor() {

    fun goToPollDetails(context: Context, pollId: String, roomId: String, isEnded: Boolean) {
        context.startActivity(
                RoomPollDetailActivity.newIntent(
                        context = context,
                        pollId = pollId,
                        roomId = roomId,
                        isEnded = isEnded,
                )
        )
    }
}
