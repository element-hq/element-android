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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.profile.ProfileService
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.members.roomMemberQueryParams
import im.vector.matrix.android.api.session.room.model.PowerLevelsContent
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.powerlevers.PowerLevelsConstants
import im.vector.matrix.android.api.session.room.powerlevers.PowerLevelsHelper
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.rx.mapOptional
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.utils.DataSource
import im.vector.riotx.core.utils.PublishDataSource
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomMemberProfileViewModel @AssistedInject constructor(@Assisted private val initialState: RoomMemberProfileViewState,
                                                             private val stringProvider: StringProvider,
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

    private val _viewEvents = PublishDataSource<RoomMemberProfileViewEvents>()
    val viewEvents: DataSource<RoomMemberProfileViewEvents> = _viewEvents

    private val room = if (initialState.roomId != null) {
        session.getRoom(initialState.roomId)
    } else {
        null
    }

    init {
        setState { copy(isMine = session.myUserId == this.userId) }
        observeIgnoredState()
        viewModelScope.launch(Dispatchers.Main) {
            // Do we have a room member for this id.
            val roomMember = withContext(Dispatchers.Default) {
                room?.getRoomMember(initialState.userId)
            }
            // If not, we look for profile info on the server
            if (room == null || roomMember == null) {
                fetchProfileInfo()
            } else {
                // otherwise we just start listening to db
                setState { copy(showAsMember = true) }
                observeRoomMemberSummary(room)
                observeRoomSummaryAndPowerLevels(room)
            }
        }
    }

    private fun observeIgnoredState() {
        session.rx().liveIgnoredUsers()
                .map { ignored ->
                    ignored.find {
                        it.userId == initialState.userId
                    } != null
                }
                .execute {
                    copy(isIgnored = it)
                }
    }

    override fun handle(action: RoomMemberProfileAction) {
        when (action) {
            RoomMemberProfileAction.RetryFetchingInfo -> fetchProfileInfo()
            is RoomMemberProfileAction.IgnoreUser     -> handleIgnoreAction()
        }
    }

    private fun observeRoomMemberSummary(room: Room) {
        val queryParams = roomMemberQueryParams {
            this.userId = QueryStringValue.Equals(initialState.userId, QueryStringValue.Case.SENSITIVE)
        }
        room.rx().liveRoomMembers(queryParams)
                .map { it.firstOrNull()?.toMatrixItem().toOptional() }
                .unwrap()
                .execute {
                    copy(userMatrixItem = it)
                }
    }

    private fun fetchProfileInfo() {
        session.rx().getProfileInfo(initialState.userId)
                .map {
                    MatrixItem.UserItem(
                            id = initialState.userId,
                            displayName = it[ProfileService.DISPLAY_NAME_KEY] as? String,
                            avatarUrl = it[ProfileService.AVATAR_URL_KEY] as? String
                    )
                }
                .execute {
                    copy(userMatrixItem = it)
                }
    }

    private fun observeRoomSummaryAndPowerLevels(room: Room) {
        val roomSummaryLive = room.rx().liveRoomSummary().unwrap()
        val powerLevelsContentLive = room.rx().liveStateEvent(EventType.STATE_ROOM_POWER_LEVELS)
                .mapOptional { it.content.toModel<PowerLevelsContent>() }
                .unwrap()

        roomSummaryLive.execute {
            copy(isRoomEncrypted = it.invoke()?.isEncrypted == true)
        }
        Observable
                .combineLatest(
                        roomSummaryLive,
                        powerLevelsContentLive,
                        BiFunction<RoomSummary, PowerLevelsContent, String> { roomSummary, powerLevelsContent ->
                            val roomName = roomSummary.toMatrixItem().getBestName()
                            val powerLevelsHelper = PowerLevelsHelper(powerLevelsContent)
                            val userPowerLevel = powerLevelsHelper.getUserPowerLevel(initialState.userId)
                            if (userPowerLevel == PowerLevelsConstants.DEFAULT_ROOM_ADMIN_LEVEL) {
                                stringProvider.getString(R.string.room_member_power_level_admin_in, roomName)
                            } else if (userPowerLevel == PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL) {
                                stringProvider.getString(R.string.room_member_power_level_moderator_in, roomName)
                            } else if (userPowerLevel == PowerLevelsConstants.DEFAULT_ROOM_USER_LEVEL) {
                                ""
                            } else {
                                stringProvider.getString(R.string.room_member_power_level_custom_in, userPowerLevel, roomName)
                            }
                        }
                ).execute {
                    copy(userPowerLevelString = it)
                }
    }

    private fun handleIgnoreAction() = withState { state ->
        val isIgnored = state.isIgnored() ?: return@withState
        _viewEvents.post(RoomMemberProfileViewEvents.Loading(stringProvider.getString(R.string.please_wait)))
        val ignoreActionCallback = object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _viewEvents.post(RoomMemberProfileViewEvents.OnIgnoreActionSuccess)
            }

            override fun onFailure(failure: Throwable) {
                _viewEvents.post(RoomMemberProfileViewEvents.Failure(failure))
            }
        }
        if (isIgnored) {
            session.unIgnoreUserIds(listOf(state.userId), ignoreActionCallback)
        } else {
            session.ignoreUserIds(listOf(initialState.userId), ignoreActionCallback)
        }
    }
}
