/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.invite

import androidx.core.view.isGone
import im.vector.app.core.platform.ButtonStateView
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState

object InviteButtonStateBinder {

    fun bind(
            acceptView: ButtonStateView,
            rejectView: ButtonStateView,
            changeMembershipState: ChangeMembershipState
    ) {
        // When a request is in progress (accept or reject), we only use the accept State button
        // We check for isSuccessful, otherwise we get a glitch the time room summaries get rebuilt

        val requestInProgress = changeMembershipState.isInProgress() || changeMembershipState.isSuccessful()
        when {
            requestInProgress -> acceptView.render(ButtonStateView.State.Loading)
            changeMembershipState is ChangeMembershipState.FailedJoining -> acceptView.render(ButtonStateView.State.Error)
            else -> acceptView.render(ButtonStateView.State.Button)
        }
        // ButtonStateView.State.Loaded not used because roomSummary will not be displayed as a room invitation anymore

        rejectView.isGone = requestInProgress

        when (changeMembershipState) {
            is ChangeMembershipState.FailedLeaving -> rejectView.render(ButtonStateView.State.Error)
            else -> rejectView.render(ButtonStateView.State.Button)
        }
    }
}
