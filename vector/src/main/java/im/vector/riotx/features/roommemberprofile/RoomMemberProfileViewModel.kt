/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.riotx.features.roommemberprofile

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.members.roomMemberQueryParams
import im.vector.matrix.android.api.session.room.model.PowerLevelsContent
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.rx.mapOptional
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.core.platform.VectorViewModel
import timber.log.Timber

class RoomMemberProfileViewModel @AssistedInject constructor(@Assisted private val initialState: RoomMemberProfileViewState,
                                                             private val session: Session)
    : VectorViewModel<RoomMemberProfileViewState, RoomMemberProfileAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomMemberProfileViewState): RoomMemberProfileViewModel
    }

    companion object : MvRxViewModelFactory<RoomMemberProfileViewModel, RoomMemberProfileViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomMemberProfileViewState): RoomMemberProfileViewModel? {
            val fragment: RoomMemberProfileFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val room = if (initialState.roomId != null) {
        session.getRoom(initialState.roomId)
    } else {
        null
    }

    init {
        setState { copy(isMine = session.myUserId == this.userId) }
        observeRoomSummary()
        observeRoomMemberSummary()
        observePowerLevel()
        fetchProfileInfoIfRequired()
    }

    private fun fetchProfileInfoIfRequired() {
        val roomMember = room?.getRoomMember(initialState.userId)
        if (roomMember != null) {
            return
        }
        session.rx().getProfileInfo(initialState.userId)
                .execute {
                    copy(profileInfo = it)
                }
    }

    override fun handle(action: RoomMemberProfileAction) {
        Timber.v("Handle $action")
    }

    private fun observeRoomSummary() {
        if (room == null) {
            return
        }
        room.rx().liveRoomSummary()
                .unwrap()
                .execute {
                    copy(roomSummary = it)
                }
    }

    private fun observeRoomMemberSummary() {
        if (room == null) {
            return
        }
        val queryParams = roomMemberQueryParams {
            this.userId = QueryStringValue.Equals(initialState.userId, QueryStringValue.Case.SENSITIVE)
        }
        room.rx().liveRoomMembers(queryParams)
                .map { it.firstOrNull().toOptional() }
                .unwrap()
                .execute {
                    copy(roomMemberSummary = it)
                }
    }

    private fun observePowerLevel() {
        if (room == null) {
            return
        }
        room.rx()
                .liveStateEvent(EventType.STATE_ROOM_POWER_LEVELS)
                .mapOptional { it.content.toModel<PowerLevelsContent>() }
                .unwrap()
                .execute {
                    copy(powerLevelsContent = it)
                }
    }


}
