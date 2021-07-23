/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces.people

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams

class SpacePeopleViewModel @AssistedInject constructor(
        @Assisted val initialState: SpacePeopleViewState,
        private val rawService: RawService,
        private val session: Session
) : VectorViewModel<SpacePeopleViewState, SpacePeopleViewAction, SpacePeopleViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: SpacePeopleViewState): SpacePeopleViewModel
    }

    companion object : MvRxViewModelFactory<SpacePeopleViewModel, SpacePeopleViewState> {
        override fun create(viewModelContext: ViewModelContext, state: SpacePeopleViewState): SpacePeopleViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: SpacePeopleViewAction) {
        when (action) {
            is SpacePeopleViewAction.ChatWith   -> handleChatWith(action)
            SpacePeopleViewAction.InviteToSpace -> handleInviteToSpace()
        }.exhaustive
    }

    private fun handleInviteToSpace() {
        _viewEvents.post(SpacePeopleViewEvents.InviteToSpace(initialState.spaceId))
    }

    private fun handleChatWith(action: SpacePeopleViewAction.ChatWith) {
        val otherUserId = action.member.userId
        if (otherUserId == session.myUserId) return
        val existingRoomId = session.getExistingDirectRoomWithUser(otherUserId)
        if (existingRoomId != null) {
            // just open it
            _viewEvents.post(SpacePeopleViewEvents.OpenRoom(existingRoomId))
            return
        }
        setState { copy(createAndInviteState = Loading()) }

        viewModelScope.launch(Dispatchers.IO) {
            val adminE2EByDefault = rawService.getElementWellknown(session.sessionParams)
                    ?.isE2EByDefault()
                    ?: true

            val roomParams = CreateRoomParams()
                    .apply {
                        invitedUserIds.add(otherUserId)
                        setDirectMessage()
                        enableEncryptionIfInvitedUsersSupportIt = adminE2EByDefault
                    }

            try {
                val roomId = session.createRoom(roomParams)
                _viewEvents.post(SpacePeopleViewEvents.OpenRoom(roomId))
                setState { copy(createAndInviteState = Success(roomId)) }
            } catch (failure: Throwable) {
                setState { copy(createAndInviteState = Fail(failure)) }
            }
        }
    }
}
