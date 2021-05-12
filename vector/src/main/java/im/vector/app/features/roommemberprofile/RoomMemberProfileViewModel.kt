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

package im.vector.app.features.roommemberprofile

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.R
import im.vector.app.core.mvrx.runCatchingToAsync
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

class RoomMemberProfileViewModel @AssistedInject constructor(@Assisted private val initialState: RoomMemberProfileViewState,
                                                             private val stringProvider: StringProvider,
                                                             private val session: Session)
    : VectorViewModel<RoomMemberProfileViewState, RoomMemberProfileAction, RoomMemberProfileViewEvents>(initialState) {

    @AssistedFactory
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
        setState {
            copy(
                    isMine = session.myUserId == this.userId,
                    userMatrixItem = room?.getRoomMember(initialState.userId)?.toMatrixItem()?.let { Success(it) } ?: Uninitialized,
                    hasReadReceipt = room?.getUserReadReceipt(initialState.userId) != null
            )
        }
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

        session.rx().liveUserCryptoDevices(initialState.userId)
                .map {
                    Pair(
                            it.fold(true, { prev, dev -> prev && dev.isVerified }),
                            it.fold(true, { prev, dev -> prev && (dev.trustLevel?.crossSigningVerified == true) })
                    )
                }
                .execute { it ->
                    copy(
                            allDevicesAreTrusted = it()?.first == true,
                            allDevicesAreCrossSignedTrusted = it()?.second == true
                    )
                }

        session.rx().liveCrossSigningInfo(initialState.userId)
                .execute {
                    copy(userMXCrossSigningInfo = it.invoke()?.getOrNull())
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
            is RoomMemberProfileAction.RetryFetchingInfo      -> handleRetryFetchProfileInfo()
            is RoomMemberProfileAction.IgnoreUser             -> handleIgnoreAction()
            is RoomMemberProfileAction.VerifyUser             -> prepareVerification()
            is RoomMemberProfileAction.ShareRoomMemberProfile -> handleShareRoomMemberProfile()
            is RoomMemberProfileAction.SetPowerLevel          -> handleSetPowerLevel(action)
            is RoomMemberProfileAction.BanOrUnbanUser         -> handleBanOrUnbanAction(action)
            is RoomMemberProfileAction.KickUser               -> handleKickAction(action)
            RoomMemberProfileAction.InviteUser                -> handleInviteAction()
        }
    }

    private fun handleSetPowerLevel(action: RoomMemberProfileAction.SetPowerLevel) = withState { state ->
        if (room == null || action.previousValue == action.newValue) {
            return@withState
        }
        val currentPowerLevelsContent = state.powerLevelsContent ?: return@withState
        val myPowerLevel = PowerLevelsHelper(currentPowerLevelsContent).getUserPowerLevelValue(session.myUserId)
        if (action.askForValidation && action.newValue >= myPowerLevel) {
            _viewEvents.post(RoomMemberProfileViewEvents.ShowPowerLevelValidation(action.previousValue, action.newValue))
        } else if (action.askForValidation && state.isMine) {
            _viewEvents.post(RoomMemberProfileViewEvents.ShowPowerLevelDemoteWarning(action.previousValue, action.newValue))
        } else {
            val newPowerLevelsContent = currentPowerLevelsContent
                    .setUserPowerLevel(state.userId, action.newValue)
                    .toContent()
            viewModelScope.launch {
                _viewEvents.post(RoomMemberProfileViewEvents.Loading())
                try {
                    room.sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, null, newPowerLevelsContent)
                    _viewEvents.post(RoomMemberProfileViewEvents.OnSetPowerLevelSuccess)
                } catch (failure: Throwable) {
                    _viewEvents.post(RoomMemberProfileViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun prepareVerification() = withState { state ->
        // Sanity
        if (state.isRoomEncrypted) {
            if (!state.isMine && state.userMXCrossSigningInfo?.isTrusted() == false) {
                // ok, let's find or create the DM room
                _viewEvents.post(RoomMemberProfileViewEvents.StartVerification(
                        userId = state.userId,
                        canCrossSign = session.cryptoService().crossSigningService().canCrossSign()
                ))
            }
        }
    }

    private fun handleInviteAction() {
        if (room == null) {
            return
        }
        viewModelScope.launch {
            try {
                _viewEvents.post(RoomMemberProfileViewEvents.Loading())
                room.invite(initialState.userId)
                _viewEvents.post(RoomMemberProfileViewEvents.OnInviteActionSuccess)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomMemberProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun handleKickAction(action: RoomMemberProfileAction.KickUser) {
        if (room == null) {
            return
        }
        viewModelScope.launch {
            try {
                _viewEvents.post(RoomMemberProfileViewEvents.Loading())
                room.kick(initialState.userId, action.reason)
                _viewEvents.post(RoomMemberProfileViewEvents.OnKickActionSuccess)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomMemberProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun handleBanOrUnbanAction(action: RoomMemberProfileAction.BanOrUnbanUser) = withState { state ->
        if (room == null) {
            return@withState
        }
        val membership = state.asyncMembership() ?: return@withState
        viewModelScope.launch {
            try {
                _viewEvents.post(RoomMemberProfileViewEvents.Loading())
                if (membership == Membership.BAN) {
                    room.unban(initialState.userId, action.reason)
                } else {
                    room.ban(initialState.userId, action.reason)
                }
                _viewEvents.post(RoomMemberProfileViewEvents.OnBanActionSuccess)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomMemberProfileViewEvents.Failure(failure))
            }
        }
    }

    private fun observeRoomMemberSummary(room: Room) {
        val queryParams = roomMemberQueryParams {
            this.userId = QueryStringValue.Equals(initialState.userId, QueryStringValue.Case.SENSITIVE)
        }
        room.rx().liveRoomMembers(queryParams)
                .map { it.firstOrNull().toOptional() }
                .unwrap()
                .execute {
                    when (it) {
                        is Loading       -> copy(userMatrixItem = Loading(), asyncMembership = Loading())
                        is Success       -> copy(
                                userMatrixItem = Success(it().toMatrixItem()),
                                asyncMembership = Success(it().membership)
                        )
                        is Fail          -> copy(userMatrixItem = Fail(it.error), asyncMembership = Fail(it.error))
                        is Uninitialized -> this
                    }
                }
    }

    private fun handleRetryFetchProfileInfo() {
        viewModelScope.launch {
            fetchProfileInfo()
        }
    }

    private suspend fun fetchProfileInfo() {
        val result = runCatchingToAsync {
            session.getProfile(initialState.userId)
                    .let {
                        MatrixItem.UserItem(
                                id = initialState.userId,
                                displayName = it[ProfileService.DISPLAY_NAME_KEY] as? String,
                                avatarUrl = it[ProfileService.AVATAR_URL_KEY] as? String
                        )
                    }
        }

        setState {
            copy(userMatrixItem = result)
        }
    }

    private fun observeRoomSummaryAndPowerLevels(room: Room) {
        val roomSummaryLive = room.rx().liveRoomSummary().unwrap()
        val powerLevelsContentLive = PowerLevelsObservableFactory(room).createObservable()

        powerLevelsContentLive
                .subscribe {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = ActionPermissions(
                            canKick = powerLevelsHelper.isUserAbleToKick(session.myUserId),
                            canBan = powerLevelsHelper.isUserAbleToBan(session.myUserId),
                            canInvite = powerLevelsHelper.isUserAbleToInvite(session.myUserId),
                            canEditPowerLevel = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_POWER_LEVELS)
                    )
                    setState { copy(powerLevelsContent = it, actionPermissions = permissions) }
                }
                .disposeOnClear()

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
                            when (val userPowerLevel = powerLevelsHelper.getUserRole(initialState.userId)) {
                                Role.Admin     -> stringProvider.getString(R.string.room_member_power_level_admin_in, roomName)
                                Role.Moderator -> stringProvider.getString(R.string.room_member_power_level_moderator_in, roomName)
                                Role.Default   -> stringProvider.getString(R.string.room_member_power_level_default_in, roomName)
                                is Role.Custom -> stringProvider.getString(R.string.room_member_power_level_custom_in, userPowerLevel.value, roomName)
                            }
                        }
                ).execute {
                    copy(userPowerLevelString = it)
                }
    }

    private fun handleIgnoreAction() = withState { state ->
        val isIgnored = state.isIgnored() ?: return@withState
        _viewEvents.post(RoomMemberProfileViewEvents.Loading())
        viewModelScope.launch {
            val event = try {
                if (isIgnored) {
                    session.unIgnoreUserIds(listOf(state.userId))
                } else {
                    session.ignoreUserIds(listOf(state.userId))
                }
                RoomMemberProfileViewEvents.OnIgnoreActionSuccess
            } catch (failure: Throwable) {
                RoomMemberProfileViewEvents.Failure(failure)
            }
            _viewEvents.post(event)
        }
    }

    private fun handleShareRoomMemberProfile() {
        session.permalinkService().createPermalink(initialState.userId)?.let { permalink ->
            _viewEvents.post(RoomMemberProfileViewEvents.ShareRoomMemberProfile(permalink))
        }
    }
}
