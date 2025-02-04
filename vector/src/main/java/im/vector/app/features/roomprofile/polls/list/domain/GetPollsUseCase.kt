/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.domain

import im.vector.app.features.roomprofile.polls.list.data.RoomPollRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class GetPollsUseCase @Inject constructor(
        private val roomPollRepository: RoomPollRepository,
) {

    fun execute(roomId: String): Flow<List<TimelineEvent>> {
        return roomPollRepository.getPolls(roomId)
                .map { it.sortedByDescending { event -> event.root.originServerTs } }
    }
}
