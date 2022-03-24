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

package im.vector.app.features.roomprofile.banned

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

class RoomBannedMemberListViewModel @AssistedInject constructor(@Assisted initialState: RoomBannedMemberListViewState,
                                                                private val stringProvider: StringProvider,
                                                                private val session: Session) :
        VectorViewModel<RoomBannedMemberListViewState, RoomBannedMemberListAction, RoomBannedMemberListViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomBannedMemberListViewModel, RoomBannedMemberListViewState> {
        override fun create(initialState: RoomBannedMemberListViewState): RoomBannedMemberListViewModel
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {

        room.flow().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }

        room.flow().liveRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.BAN) })
                .execute {
                    copy(
                            bannedMemberSummaries = it
                    )
                }

        val powerLevelsContentLive = PowerLevelsFlowFactory(room).createFlow()

        powerLevelsContentLive
                .setOnEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    copy(canUserBan = powerLevelsHelper.isUserAbleToBan(session.myUserId))
                }
    }

    companion object : MavericksViewModelFactory<RoomBannedMemberListViewModel, RoomBannedMemberListViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: RoomBannedMemberListAction) {
        when (action) {
            is RoomBannedMemberListAction.QueryInfo -> onQueryBanInfo(action.roomMemberSummary)
            is RoomBannedMemberListAction.UnBanUser -> unBanUser(action.roomMemberSummary)
            is RoomBannedMemberListAction.Filter    -> handleFilter(action)
        }
    }

    private fun handleFilter(action: RoomBannedMemberListAction.Filter) {
        setState {
            copy(
                    filter = action.filter
            )
        }
    }

    private fun onQueryBanInfo(roomMemberSummary: RoomMemberSummary) {
        val bannedEvent = room.getStateEvent(EventType.STATE_ROOM_MEMBER, QueryStringValue.Equals(roomMemberSummary.userId))
        val content = bannedEvent?.getClearContent().toModel<RoomMemberContent>()
        if (content?.membership != Membership.BAN) {
            // may be report error?
            return
        }

        val reason = content.reason
        val bannedBy = bannedEvent?.senderId ?: return

        _viewEvents.post(RoomBannedMemberListViewEvents.ShowBannedInfo(bannedBy, reason ?: "", roomMemberSummary))
    }

    private fun unBanUser(roomMemberSummary: RoomMemberSummary) {
        setState {
            copy(onGoingModerationAction = this.onGoingModerationAction + roomMemberSummary.userId)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                room.unban(roomMemberSummary.userId, null)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomBannedMemberListViewEvents.ToastError(stringProvider.getString(R.string.failed_to_unban)))
            } finally {
                setState {
                    copy(
                            onGoingModerationAction = onGoingModerationAction - roomMemberSummary.userId
                    )
                }
            }
        }
    }
}
