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

package im.vector.app.features.spaces.invite

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class SpaceInviteBottomSheetViewModel @AssistedInject constructor(
        @Assisted private val initialState: SpaceInviteBottomSheetState,
        private val session: Session,
        private val errorFormatter: ErrorFormatter
) : VectorViewModel<SpaceInviteBottomSheetState, SpaceInviteBottomSheetAction, SpaceInviteBottomSheetEvents>(initialState) {

    init {
        session.getRoomSummary(initialState.spaceId)?.let { roomSummary ->

            val knownMembers = roomSummary.otherMemberIds.filter {
                session.getExistingDirectRoomWithUser(it) != null
            }.mapNotNull { session.getUser(it) }
            // put one with avatar first, and take 5
            val peopleYouKnow = (knownMembers.filter { it.avatarUrl != null } + knownMembers.filter { it.avatarUrl == null })
                    .take(5)

            setState {
                copy(
                        summary = Success(roomSummary),
                        inviterUser = roomSummary.inviterId?.let { session.getUser(it) }?.let { Success(it) } ?: Uninitialized,
                        peopleYouKnow = Success(peopleYouKnow)
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: SpaceInviteBottomSheetState): SpaceInviteBottomSheetViewModel
    }

    companion object : MvRxViewModelFactory<SpaceInviteBottomSheetViewModel, SpaceInviteBottomSheetState> {

        override fun create(viewModelContext: ViewModelContext, state: SpaceInviteBottomSheetState): SpaceInviteBottomSheetViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: SpaceInviteBottomSheetAction) {
        when (action) {
            SpaceInviteBottomSheetAction.DoJoin   -> {
                setState { copy(joinActionState = Loading()) }
                session.coroutineScope.launch(Dispatchers.IO) {
                    try {
                        session.getRoom(initialState.spaceId)?.join()
                        setState { copy(joinActionState = Success(Unit)) }
                    } catch (failure: Throwable) {
                        setState { copy(joinActionState = Fail(failure)) }
                        _viewEvents.post(SpaceInviteBottomSheetEvents.ShowError(errorFormatter.toHumanReadable(failure)))
                    }
                }
            }
            SpaceInviteBottomSheetAction.DoReject -> {
                setState { copy(rejectActionState = Loading()) }
                session.coroutineScope.launch(Dispatchers.IO) {
                    try {
                        session.getRoom(initialState.spaceId)?.leave()
                        setState { copy(rejectActionState = Success(Unit)) }
                    } catch (failure: Throwable) {
                        setState { copy(rejectActionState = Fail(failure)) }
                        _viewEvents.post(SpaceInviteBottomSheetEvents.ShowError(errorFormatter.toHumanReadable(failure)))
                    }
                }
            }
        }
    }
}
