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

import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPollsUseCase @Inject constructor() {

    fun execute(): Flow<List<PollSummary>> {
        // TODO unmock and add unit tests
        return flowOf(getActivePolls() + getEndedPolls())
                .map { it.sortedByDescending { poll -> poll.creationTimestamp } }
    }

    private fun getActivePolls(): List<PollSummary.ActivePoll> {
        return listOf(
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
    }

    private fun getEndedPolls(): List<PollSummary.EndedPoll> {
        return listOf(
                PollSummary.EndedPoll(
                        id = "id1-ended",
                        // 2022/06/28 UTC+1
                        creationTimestamp = 1656367200000,
                        title = "Which charity would you like to support?",
                        totalVotes = 22,
                        winnerOptions = listOf(
                                PollOptionViewState.PollEnded(
                                        optionId = "id1",
                                        optionAnswer = "Cancer research",
                                        voteCount = 13,
                                        votePercentage = 13 / 22.0,
                                        isWinner = true,
                                )
                        ),
                ),
                PollSummary.EndedPoll(
                        id = "id2-ended",
                        // 2022/06/26 UTC+1
                        creationTimestamp = 1656194400000,
                        title = "Where should we do the offsite?",
                        totalVotes = 92,
                        winnerOptions = listOf(
                                PollOptionViewState.PollEnded(
                                        optionId = "id1",
                                        optionAnswer = "Hawaii",
                                        voteCount = 43,
                                        votePercentage = 43 / 92.0,
                                        isWinner = true,
                                )
                        ),
                ),
                PollSummary.EndedPoll(
                        id = "id3-ended",
                        // 2022/06/24 UTC+1
                        creationTimestamp = 1656021600000,
                        title = "What type of food should we have at the party?",
                        totalVotes = 22,
                        winnerOptions = listOf(
                                PollOptionViewState.PollEnded(
                                        optionId = "id1",
                                        optionAnswer = "Brazilian",
                                        voteCount = 13,
                                        votePercentage = 13 / 22.0,
                                        isWinner = true,
                                )
                        ),
                ),
        )
    }
}
