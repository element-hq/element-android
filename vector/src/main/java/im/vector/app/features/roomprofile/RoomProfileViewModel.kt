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
 *
 */

package im.vector.app.features.roomprofile

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.ShortcutCreator
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap
import java.util.UUID

class RoomProfileViewModel @AssistedInject constructor(
        @Assisted private val initialState: RoomProfileViewState,
        private val stringProvider: StringProvider,
        private val shortcutCreator: ShortcutCreator,
        private val session: Session
) : VectorViewModel<RoomProfileViewState, RoomProfileAction, RoomProfileViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomProfileViewState): RoomProfileViewModel
    }

    companion object : MvRxViewModelFactory<RoomProfileViewModel, RoomProfileViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomProfileViewState): RoomProfileViewModel? {
            val fragment: RoomProfileFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.roomProfileViewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
    }

    private fun observeRoomSummary() {
        val rxRoom = room.rx()
        rxRoom.liveRoomSummary()
                .unwrap()
                .execute {
                    copy(roomSummary = it)
                }

        val powerLevelsContentLive = PowerLevelsObservableFactory(room).createObservable()

        powerLevelsContentLive
                .subscribe {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    setState {
                        copy(canChangeAvatar = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_AVATAR))
                    }
                }
                .disposeOnClear()

        rxRoom.liveRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.BAN) })
                .execute {
                    copy(
                            bannedMembership = it
                    )
                }
    }

    override fun handle(action: RoomProfileAction) {
        when (action) {
            RoomProfileAction.LeaveRoom                      -> handleLeaveRoom()
            is RoomProfileAction.ChangeRoomNotificationState -> handleChangeNotificationMode(action)
            is RoomProfileAction.ShareRoomProfile            -> handleShareRoomProfile()
            is RoomProfileAction.ChangeRoomAvatar            -> handleChangeAvatar(action)
            RoomProfileAction.CreateShortcut                 -> handleCreateShortcut()
        }.exhaustive
    }

    private fun handleCreateShortcut() {
        viewModelScope.launch(Dispatchers.IO) {
            withState { state ->
                state.roomSummary()
                        ?.let { shortcutCreator.create(it) }
                        ?.let { _viewEvents.post(RoomProfileViewEvents.OnShortcutReady(it)) }
            }
        }
    }

    private fun handleChangeNotificationMode(action: RoomProfileAction.ChangeRoomNotificationState) {
        room.setRoomNotificationState(action.notificationState, object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        })
    }

    private fun handleLeaveRoom() {
        _viewEvents.post(RoomProfileViewEvents.Loading(stringProvider.getString(R.string.room_profile_leaving_room)))
        room.leave(null, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // Do nothing, we will be closing the room automatically when it will get back from sync
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        })
    }

    private fun handleShareRoomProfile() {
        session.permalinkService().createRoomPermalink(initialState.roomId)
                ?.let { permalink ->
                    _viewEvents.post(RoomProfileViewEvents.ShareRoomProfile(permalink))
                }
    }

    private fun handleChangeAvatar(action: RoomProfileAction.ChangeRoomAvatar) {
        _viewEvents.post(RoomProfileViewEvents.Loading())
        room.rx().updateAvatar(action.uri, action.fileName ?: UUID.randomUUID().toString())
                .subscribe(
                        {
                            _viewEvents.post(RoomProfileViewEvents.OnChangeAvatarSuccess)
                        },
                        {
                            _viewEvents.post(RoomProfileViewEvents.Failure(it))
                        }
                )
                .disposeOnClear()
    }
}
