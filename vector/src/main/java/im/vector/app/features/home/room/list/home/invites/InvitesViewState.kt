/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.invites

import android.graphics.drawable.Drawable
import androidx.paging.PagedList
import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class InvitesViewState(
        val roomMembershipChanges: Map<String, ChangeMembershipState> = emptyMap(),
) : MavericksState

sealed interface InvitesContentState {
    object Loading : InvitesContentState
    data class Empty(
            val title: CharSequence,
            val image: Drawable?,
            val message: CharSequence
    ) : InvitesContentState

    data class Content(val content: PagedList<RoomSummary>) : InvitesContentState
    data class Error(val throwable: Throwable) : InvitesContentState
}
