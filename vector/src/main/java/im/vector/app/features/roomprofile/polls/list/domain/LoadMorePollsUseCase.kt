/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.domain

import im.vector.app.features.roomprofile.polls.list.data.RoomPollRepository
import org.matrix.android.sdk.api.session.room.poll.LoadedPollsStatus
import javax.inject.Inject

class LoadMorePollsUseCase @Inject constructor(
        private val roomPollRepository: RoomPollRepository,
) {

    suspend fun execute(roomId: String): LoadedPollsStatus {
        return roomPollRepository.loadMorePolls(roomId)
    }
}
