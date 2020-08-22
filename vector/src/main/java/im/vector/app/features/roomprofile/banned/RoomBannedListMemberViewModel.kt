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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap
import im.vector.app.R
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoomBannedListMemberViewModel @AssistedInject constructor(@Assisted initialState: RoomBannedMemberListViewState,
                                                                private val stringProvider: StringProvider,
                                                                private val session: Session)
    : VectorViewModel<RoomBannedMemberListViewState, RoomBannedListMemberAction, RoomBannedViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomBannedMemberListViewState): RoomBannedListMemberViewModel
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        val rxRoom = room.rx()

        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }

        rxRoom.liveRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.BAN) })
                .execute {
                    copy(
                            bannedMemberSummaries = it
                    )
                }

        val powerLevelsContentLive = PowerLevelsObservableFactory(room).createObservable()

        powerLevelsContentLive.subscribe {
            val powerLevelsHelper = PowerLevelsHelper(it)
            setState { copy(canUserBan = powerLevelsHelper.isUserAbleToBan(session.myUserId)) }
        }.disposeOnClear()
    }

    companion object : MvRxViewModelFactory<RoomBannedListMemberViewModel, RoomBannedMemberListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomBannedMemberListViewState): RoomBannedListMemberViewModel? {
            val fragment: RoomBannedMemberListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    override fun handle(action: RoomBannedListMemberAction) {
        when (action) {
            is RoomBannedListMemberAction.QueryInfo -> onQueryBanInfo(action.roomMemberSummary)
            is RoomBannedListMemberAction.UnBanUser -> unBanUser(action.roomMemberSummary)
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

        _viewEvents.post(RoomBannedViewEvents.ShowBannedInfo(bannedBy, reason ?: "", roomMemberSummary))
    }

    private fun unBanUser(roomMemberSummary: RoomMemberSummary) {
        setState {
            copy(onGoingModerationAction = this.onGoingModerationAction + roomMemberSummary.userId)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                awaitCallback<Unit> {
                    room.unban(roomMemberSummary.userId, null, it)
                }
            } catch (failure: Throwable) {
                _viewEvents.post(RoomBannedViewEvents.ToastError(stringProvider.getString(R.string.failed_to_unban)))
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
