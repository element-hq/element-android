/*
 * Copyright 2019 New Vector Ltd
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
 */

package im.vector.riotx.features.roomprofile.members

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.members.roomMemberQueryParams
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.utils.DataSource
import im.vector.riotx.core.utils.PublishDataSource
import im.vector.riotx.features.roomprofile.RoomProfileViewEvents

class RoomMemberListViewModel @AssistedInject constructor(@Assisted initialState: RoomMemberListViewState,
                                                          private val session: Session)
    : VectorViewModel<RoomMemberListViewState, RoomMemberListAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomMemberListViewState): RoomMemberListViewModel
    }

    companion object : MvRxViewModelFactory<RoomMemberListViewModel, RoomMemberListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomMemberListViewState): RoomMemberListViewModel? {
            val fragment: RoomMemberListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomMembers()
        observeRoomSummary()
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    private fun observeRoomMembers() {
        val queryParams = roomMemberQueryParams {
            displayName = QueryStringValue.IsNotEmpty
            memberships = Membership.activeMemberships()
        }
        room.rx()
                .liveRoomMembers(queryParams)
                .execute {
                    copy(roomMembers = it)
                }
    }

    override fun handle(action: RoomMemberListAction) {

    }

}
