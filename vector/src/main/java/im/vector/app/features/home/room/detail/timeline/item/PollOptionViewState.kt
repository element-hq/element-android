/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

sealed class PollOptionViewState(
        open val optionId: String,
        open val optionAnswer: String
) {
    /**
     * Represents a poll that is not sent to the server yet.
     */
    data class PollSending(
            override val optionId: String,
            override val optionAnswer: String
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that is sent but not voted by the user.
     */
    data class PollReady(
            override val optionId: String,
            override val optionAnswer: String
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that user already voted.
     */
    data class PollVoted(
            override val optionId: String,
            override val optionAnswer: String,
            val voteCount: Int,
            val votePercentage: Double,
            val isSelected: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represents a poll that is ended.
     */
    data class PollEnded(
            override val optionId: String,
            override val optionAnswer: String,
            val voteCount: Int,
            val votePercentage: Double,
            val isWinner: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)

    /**
     * Represent a poll that is undisclosed, votes will be hidden until the poll is ended.
     */
    data class PollUndisclosed(
            override val optionId: String,
            override val optionAnswer: String,
            val isSelected: Boolean
    ) : PollOptionViewState(optionId, optionAnswer)
}
