/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.room.model

import com.squareup.moshi.JsonClass

/**
 * Contains an aggregated summary info of the poll response.
 * Put pre-computed info that you want to access quickly without having
 * to go through all references events
 */
@JsonClass(generateAdapter = true)
data class PollSummaryContent(
        val myVote: String? = null,
        // List of VoteInfo, list is constructed so that there is only one vote by user
        // And that optionIndex is valid
        val votes: List<VoteInfo>? = null,
        val votesSummary: Map<String, VoteSummary>? = null,
        val totalVotes: Int = 0,
        val winnerVoteCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class VoteSummary(
        val total: Int = 0,
        val percentage: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class VoteInfo(
        val userId: String,
        val option: String,
        val voteTimestamp: Long
)
