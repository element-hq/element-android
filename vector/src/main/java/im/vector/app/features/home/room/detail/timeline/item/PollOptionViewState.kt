/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.item

sealed class PollOptionViewState(open val optionId: String,
                                 open val optionAnswer: String) {
    /**
     * Represents a poll that is not sent to the server yet.
     */
    data class PollSending(override val optionId: String,
                           override val optionAnswer: String
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that is sent but not voted by the user
     */
    data class PollReady(override val optionId: String,
                         override val optionAnswer: String
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that user already voted.
     */
    data class PollVoted(override val optionId: String,
                         override val optionAnswer: String,
                         val voteCount: Int,
                         val votePercentage: Double,
                         val isSelected: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that is ended.
     */
    data class PollEnded(override val optionId: String,
                         override val optionAnswer: String,
                         val voteCount: Int,
                         val votePercentage: Double,
                         val isWinner: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represent a poll that is undisclosed, votes will be hidden until the poll is ended.
     */
    data class PollUndisclosed(override val optionId: String,
                               override val optionAnswer: String,
                               val isSelected: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)
}
