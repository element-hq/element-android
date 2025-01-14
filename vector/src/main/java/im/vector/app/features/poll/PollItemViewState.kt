/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.poll

import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState

data class PollItemViewState(
        val question: String,
        val votesStatus: String,
        val canVote: Boolean,
        val optionViewStates: List<PollOptionViewState>?,
)
