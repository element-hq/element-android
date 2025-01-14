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

/**
 * Sync the polls of a given room from last manual loading if any (see LoadMorePollsUseCase) until now.
 * Resume or start loading more to have at least a complete load.
 */
class SyncPollsUseCase @Inject constructor(
        private val roomPollRepository: RoomPollRepository,
        private val getLoadedPollsStatusUseCase: GetLoadedPollsStatusUseCase,
        private val loadMorePollsUseCase: LoadMorePollsUseCase,
) {

    suspend fun execute(roomId: String): LoadedPollsStatus {
        roomPollRepository.syncPolls(roomId)
        val loadedStatus = getLoadedPollsStatusUseCase.execute(roomId)
        return if (loadedStatus.hasCompletedASyncBackward) {
            loadedStatus
        } else {
            loadMorePollsUseCase.execute(roomId)
        }
    }
}
