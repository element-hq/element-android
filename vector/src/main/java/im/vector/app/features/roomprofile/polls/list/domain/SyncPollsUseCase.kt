/*
 * Copyright (c) 2023 New Vector Ltd
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
