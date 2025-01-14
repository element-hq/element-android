/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.ui

import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState

sealed interface PollSummary {
    val id: String
    val creationTimestamp: Long
    val title: String

    data class ActivePoll(
            override val id: String,
            override val creationTimestamp: Long,
            override val title: String,
    ) : PollSummary

    data class EndedPoll(
            override val id: String,
            override val creationTimestamp: Long,
            override val title: String,
            val totalVotes: Int,
            val winnerOptions: List<PollOptionViewState.PollEnded>,
    ) : PollSummary
}
