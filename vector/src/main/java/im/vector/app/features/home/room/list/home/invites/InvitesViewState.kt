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
