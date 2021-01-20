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

package im.vector.app.features.createdirect

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import im.vector.app.features.userdirectory.PendingInvitee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.rx.rx

class CreateDirectRoomViewModel @AssistedInject constructor(@Assisted
                                                            initialState: CreateDirectRoomViewState,
                                                            private val rawService: RawService,
                                                            val session: Session)
    : VectorViewModel<CreateDirectRoomViewState, CreateDirectRoomAction, CreateDirectRoomViewEvents>(initialState) {

    @AssistedFactory
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
            is CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers -> onSubmitInvitees(action)
        }.exhaustive
    }

    /**
     * If users already have a DM room then navigate to it instead of creating a new room.
     */
    private fun onSubmitInvitees(action: CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers) {
        if (action.existingDmRoomId != null) {
            // Do not create a new DM, just tell that the creation is successful by passing the existing roomId
            setState {
                copy(createAndInviteState = Success(action.existingDmRoomId))
            }
        } else {
            // Create the DM
            createRoomAndInviteSelectedUsers(action.invitees)
        }
    }

    private fun createRoomAndInviteSelectedUsers(invitees: Set<PendingInvitee>) {
        viewModelScope.launch(Dispatchers.IO) {
            val adminE2EByDefault = rawService.getElementWellknown(session.myUserId)
                    ?.isE2EByDefault()
                    ?: true

            val roomParams = CreateRoomParams()
                    .apply {
                        invitees.forEach {
                            when (it) {
                                is PendingInvitee.UserPendingInvitee     -> invitedUserIds.add(it.user.userId)
                                is PendingInvitee.ThreePidPendingInvitee -> invite3pids.add(it.threePid)
                            }.exhaustive
                        }
                        setDirectMessage()
                        enableEncryptionIfInvitedUsersSupportIt = adminE2EByDefault
                    }

            session.rx()
                    .createRoom(roomParams)
                    .execute {
                        copy(createAndInviteState = it)
                    }
        }
    }
}
