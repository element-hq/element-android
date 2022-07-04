/*
 * Copyright (c) 2020 New Vector Ltd
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
