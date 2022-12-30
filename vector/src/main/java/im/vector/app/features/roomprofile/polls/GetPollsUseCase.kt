/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPollsUseCase @Inject constructor() {

    fun execute(filter: RoomPollsFilter): Flow<List<PollSummary>> {
        // TODO unmock and add unit tests
        return when (filter) {
            RoomPollsFilter.ACTIVE -> getActivePolls()
            RoomPollsFilter.ENDED -> emptyFlow()
        }.map { it.sortedByDescending { poll -> poll.creationTimestamp } }
    }

    private fun getActivePolls(): Flow<List<PollSummary.ActivePoll>> {
        return flowOf(
                listOf(
                        PollSummary.ActivePoll(
                                id = "id1",
                                // 2022/06/28 UTC+1
                                creationTimestamp = 1656367200000,
                                title = "Which charity would you like to support?"
                        ),
                        PollSummary.ActivePoll(
                                id = "id2",
                                // 2022/06/26 UTC+1
                                creationTimestamp = 1656194400000,
                                title = "Which sport should the pupils do this year?"
                        ),
                        PollSummary.ActivePoll(
                                id = "id3",
                                // 2022/06/24 UTC+1
                                creationTimestamp = 1656021600000,
                                title = "What type of food should we have at the party?"
                        ),
                        PollSummary.ActivePoll(
                                id = "id4",
                                // 2022/06/22 UTC+1
                                creationTimestamp = 1655848800000,
                                title = "What film should we show at the end of the year party?"
                        ),
                )
        )
    }
}
