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

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.ShortcutCreator
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.state.isPublic
import org.matrix.android.sdk.flow.FlowRoom
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import org.matrix.android.sdk.flow.unwrap

class RoomProfileViewModel @AssistedInject constructor(
        @Assisted private val initialState: RoomProfileViewState,
        private val stringProvider: StringProvider,
        private val shortcutCreator: ShortcutCreator,
        private val session: Session
) : VectorViewModel<RoomProfileViewState, RoomProfileAction, RoomProfileViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomProfileViewModel, RoomProfileViewState> {
        override fun create(initialState: RoomProfileViewState): RoomProfileViewModel
    }

    companion object : MavericksViewModelFactory<RoomProfileViewModel, RoomProfileViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        val flowRoom = room.flow()
        observeRoomSummary(flowRoom)
        observeRoomCreateContent(flowRoom)
        observeBannedRoomMembers(flowRoom)
        observePermissions()
    }

    private fun observeRoomCreateContent(flowRoom: FlowRoom) {
        flowRoom.liveStateEvent(EventType.STATE_ROOM_CREATE, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomCreateContent>() }
                .unwrap()
                .execute { async ->
                    copy(
                            roomCreateContent = async,
                            // This is a shortcut, we should do the next lines elsewhere, but keep it like that for the moment.
                            recommendedRoomVersion = room.getRecommendedVersion(),
                            isUsingUnstableRoomVersion = room.isUsingUnstableRoomVersion(),
                            canUpgradeRoom = room.userMayUpgradeRoom(session.myUserId),
                            isTombstoned = room.getStateEvent(EventType.STATE_ROOM_TOMBSTONE) != null
                    )
                }
    }

    private fun observeRoomSummary(flowRoom: FlowRoom) {
        flowRoom.liveRoomSummary()
                .unwrap()
                .execute {
                    copy(roomSummary = it)
                }
    }

    private fun observeBannedRoomMembers(flowRoom: FlowRoom) {
        flowRoom.liveRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.BAN) })
                .execute {
                    copy(bannedMembership = it)
                }
    }

    private fun observePermissions() {
        PowerLevelsFlowFactory(room)
                .createFlow()
                .setOnEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = RoomProfileViewState.ActionPermissions(
                            canEnableEncryption = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_ENCRYPTION)
                    )
                    copy(actionPermissions = permissions)
                }
    }

    override fun handle(action: RoomProfileAction) {
        when (action) {
            is RoomProfileAction.EnableEncryption            -> handleEnableEncryption()
            RoomProfileAction.LeaveRoom                      -> handleLeaveRoom()
            is RoomProfileAction.ChangeRoomNotificationState -> handleChangeNotificationMode(action)
            is RoomProfileAction.ShareRoomProfile            -> handleShareRoomProfile()
            RoomProfileAction.CreateShortcut                 -> handleCreateShortcut()
        }.exhaustive
    }

    fun isPublicRoom(): Boolean {
        return room.isPublic()
    }

    private fun handleEnableEncryption() {
        postLoading(true)

        viewModelScope.launch {
            val result = runCatching { room.enableEncryption() }
            postLoading(false)
            result.onFailure { failure ->
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun postLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
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
        viewModelScope.launch {
            try {
                room.setRoomNotificationState(action.notificationState)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun handleLeaveRoom() {
        _viewEvents.post(RoomProfileViewEvents.Loading(stringProvider.getString(R.string.room_profile_leaving_room)))
        viewModelScope.launch {
            try {
                room.leave(null)
                // Do nothing, we will be closing the room automatically when it will get back from sync
            } catch (failure: Throwable) {
                _viewEvents.post(RoomProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun handleShareRoomProfile() {
        session.permalinkService().createRoomPermalink(initialState.roomId)
                ?.let { permalink ->
                    _viewEvents.post(RoomProfileViewEvents.ShareRoomProfile(permalink))
                }
    }
}
