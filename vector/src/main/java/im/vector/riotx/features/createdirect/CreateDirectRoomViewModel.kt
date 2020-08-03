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

package im.vector.riotx.features.createdirect

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.rx.rx
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.userdirectory.PendingInvitee

class CreateDirectRoomViewModel @AssistedInject constructor(@Assisted
                                                            initialState: CreateDirectRoomViewState,
                                                            private val session: Session)
    : VectorViewModel<CreateDirectRoomViewState, CreateDirectRoomAction, CreateDirectRoomViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CreateDirectRoomViewState): CreateDirectRoomViewModel
    }

    companion object : MvRxViewModelFactory<CreateDirectRoomViewModel, CreateDirectRoomViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CreateDirectRoomViewState): CreateDirectRoomViewModel? {
            val activity: CreateDirectRoomActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.createDirectRoomViewModelFactory.create(state)
        }
    }

    override fun handle(action: CreateDirectRoomAction) {
        when (action) {
            is CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers -> createRoomAndInviteSelectedUsers(action.invitees)
        }
    }

    private fun createRoomAndInviteSelectedUsers(invitees: Set<PendingInvitee>) {
        val roomParams = CreateRoomParams()
                .apply {
                    invitees.forEach {
                        when (it) {
                            is PendingInvitee.UserPendingInvitee     -> invitedUserIds.add(it.user.userId)
                            is PendingInvitee.ThreePidPendingInvitee -> invite3pids.add(it.threePid)
                        }.exhaustive
                    }
                    setDirectMessage()
                    enableEncryptionIfInvitedUsersSupportIt = session.getHomeServerCapabilities().adminE2EByDefault
                }

        session.rx()
                .createRoom(roomParams)
                .execute {
                    copy(createAndInviteState = it)
                }
    }
}
