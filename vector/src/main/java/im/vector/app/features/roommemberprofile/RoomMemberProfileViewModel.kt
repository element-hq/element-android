/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roommemberprofile

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.mvrx.runCatchingToAsync
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.createdirect.DirectRoomHelper
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import im.vector.app.features.powerlevel.PowerLevelsFlowFactory
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomEncryptionAlgorithm
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

class RoomMemberProfileViewModel @AssistedInject constructor(
        @Assisted private val initialState: RoomMemberProfileViewState,
        private val stringProvider: StringProvider,
        private val matrixItemColorProvider: MatrixItemColorProvider,
        private val directRoomHelper: DirectRoomHelper,
        private val session: Session
) : VectorViewModel<RoomMemberProfileViewState, RoomMemberProfileAction, RoomMemberProfileViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomMemberProfileViewModel, RoomMemberProfileViewState> {
        override fun create(initialState: RoomMemberProfileViewState): RoomMemberProfileViewModel
    }

    companion object : MavericksViewModelFactory<RoomMemberProfileViewModel, RoomMemberProfileViewState> by hiltMavericksViewModelFactory()

    private val room = if (initialState.roomId != null) {
        session.getRoom(initialState.roomId)
    } else {
        null
    }

    init {
        setState {
            copy(
                    isMine = session.myUserId == this.userId,
                    userMatrixItem = room?.membershipService()?.getRoomMember(initialState.userId)?.toMatrixItem()?.let { Success(it) } ?: Uninitialized,
                    hasReadReceipt = room?.readService()?.getUserReadReceipt(initialState.userId) != null,
                    isSpace = room?.roomSummary()?.roomType == RoomType.SPACE
            )
        }
        observeIgnoredState()
        observeAccountData()
        viewModelScope.launch(Dispatchers.Main) {
            // Do we have a room member for this id.
            val roomMember = withContext(Dispatchers.Default) {
                room?.membershipService()?.getRoomMember(initialState.userId)
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

        session.flow().liveUserCryptoDevices(initialState.userId)
                .map {
                    Pair(
                            it.fold(true) { prev, dev -> prev && dev.isVerified },
                            it.fold(true) { prev, dev -> prev && (dev.trustLevel?.crossSigningVerified == true) }
                    )
                }
                .execute {
                    copy(
                            allDevicesAreTrusted = it()?.first == true,
                            allDevicesAreCrossSignedTrusted = it()?.second == true
                    )
                }

        session.flow().liveCrossSigningInfo(initialState.userId)
                .execute {
                    copy(userMXCrossSigningInfo = it.invoke()?.getOrNull())
                }
    }

    private fun observeAccountData() {
        session.flow()
                .liveUserAccountData(UserAccountDataTypes.TYPE_OVERRIDE_COLORS)
                .unwrap()
                .onEach {
                    val newUserColor = it.content.toModel<Map<String, String>>()?.get(initialState.userId)
                    setState {
                        copy(
                                userColorOverride = newUserColor
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    private fun observeIgnoredState() {
        session.flow().liveIgnoredUsers()
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
            is RoomMemberProfileAction.RetryFetchingInfo -> handleRetryFetchProfileInfo()
            is RoomMemberProfileAction.IgnoreUser -> handleIgnoreAction()
            is RoomMemberProfileAction.ReportUser -> handleReportAction()
            is RoomMemberProfileAction.VerifyUser -> prepareVerification()
            is RoomMemberProfileAction.ShareRoomMemberProfile -> handleShareRoomMemberProfile()
            is RoomMemberProfileAction.SetPowerLevel -> handleSetPowerLevel(action)
            is RoomMemberProfileAction.BanOrUnbanUser -> handleBanOrUnbanAction(action)
            is RoomMemberProfileAction.KickUser -> handleKickAction(action)
            RoomMemberProfileAction.InviteUser -> handleInviteAction()
            is RoomMemberProfileAction.SetUserColorOverride -> handleSetUserColorOverride(action)
            is RoomMemberProfileAction.OpenOrCreateDm -> handleOpenOrCreateDm(action)
        }
    }

    private fun handleReportAction() {
        room ?: return
        viewModelScope.launch {
            val event = try {
                // The API needs an Event, use user state event if available (it should always be available)
                val userStateEventId = room.stateService()
                        .getStateEvent(EventType.STATE_ROOM_MEMBER, QueryStringValue.Equals(initialState.userId))
                        ?.eventId
                // If not found fallback to the latest event
                val eventId = (userStateEventId ?: room.roomSummary()?.latestPreviewableEvent?.eventId) ?: return@launch
                room.reportingService()
                        .reportContent(
                                eventId = eventId,
                                score = -100,
                                reason = "Reporting user ${initialState.userId}"
                        )
                RoomMemberProfileViewEvents.OnReportActionSuccess
            } catch (failure: Throwable) {
                RoomMemberProfileViewEvents.Failure(failure)
            }
            _viewEvents.post(event)
        }
    }

    private fun handleOpenOrCreateDm(action: RoomMemberProfileAction.OpenOrCreateDm) {
        viewModelScope.launch {
            _viewEvents.post(RoomMemberProfileViewEvents.Loading())
            val roomId = try {
                directRoomHelper.ensureDMExists(action.userId)
            } catch (failure: Throwable) {
                _viewEvents.post(RoomMemberProfileViewEvents.Failure(failure))
                return@launch
            }
            if (roomId != initialState.roomId) {
                _viewEvents.post(RoomMemberProfileViewEvents.OpenRoom(roomId = roomId))
            } else {
                // Just go back to the previous screen (timeline)
                _viewEvents.post(RoomMemberProfileViewEvents.GoBack)
            }
        }
    }

    private fun handleSetUserColorOverride(action: RoomMemberProfileAction.SetUserColorOverride) {
        val newOverrideColorSpecs = session.accountDataService()
                .getUserAccountDataEvent(UserAccountDataTypes.TYPE_OVERRIDE_COLORS)
                ?.content
                ?.toModel<Map<String, String>>()
                .orEmpty()
                .toMutableMap()
        if (matrixItemColorProvider.setOverrideColor(initialState.userId, action.newColorSpec)) {
            newOverrideColorSpecs[initialState.userId] = action.newColorSpec
        } else {
            newOverrideColorSpecs.remove(initialState.userId)
        }
        viewModelScope.launch {
            try {
                session.accountDataService().updateUserAccountData(
                        type = UserAccountDataTypes.TYPE_OVERRIDE_COLORS,
                        content = newOverrideColorSpecs
                )
            } catch (failure: Throwable) {
                _viewEvents.post(RoomMemberProfileViewEvents.Failure(failure))
            }
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
                    room.stateService().sendStateEvent(EventType.STATE_ROOM_POWER_LEVELS, stateKey = "", newPowerLevelsContent)
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
                _viewEvents.post(
                        RoomMemberProfileViewEvents.StartVerification(
                                userId = state.userId,
                                canCrossSign = session.cryptoService().crossSigningService().canCrossSign()
                        )
                )
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
                room.membershipService().invite(initialState.userId)
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
                room.membershipService().remove(initialState.userId, action.reason)
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
                    room.membershipService().unban(initialState.userId, action.reason)
                } else {
                    room.membershipService().ban(initialState.userId, action.reason)
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
        room.flow().liveRoomMembers(queryParams)
                .map { it.firstOrNull().toOptional() }
                .unwrap()
                .execute {
                    when (it) {
                        is Loading -> copy(userMatrixItem = Loading(), asyncMembership = Loading())
                        is Success -> copy(
                                userMatrixItem = Success(it().toMatrixItem()),
                                asyncMembership = Success(it().membership)
                        )
                        is Fail -> copy(userMatrixItem = Fail(it.error), asyncMembership = Fail(it.error))
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
            session.profileService()
                    .getProfile(initialState.userId)
                    .let { User.fromJson(initialState.userId, it) }
                    .toMatrixItem()
        }

        setState {
            copy(userMatrixItem = result)
        }
    }

    private fun observeRoomSummaryAndPowerLevels(room: Room) {
        val roomSummaryLive = room.flow().liveRoomSummary().unwrap()
        val powerLevelsContentLive = PowerLevelsFlowFactory(room).createFlow()

        powerLevelsContentLive
                .onEach {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    val permissions = ActionPermissions(
                            canKick = powerLevelsHelper.isUserAbleToKick(session.myUserId),
                            canBan = powerLevelsHelper.isUserAbleToBan(session.myUserId),
                            canInvite = powerLevelsHelper.isUserAbleToInvite(session.myUserId),
                            canEditPowerLevel = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_POWER_LEVELS)
                    )
                    setState {
                        copy(powerLevelsContent = it, actionPermissions = permissions)
                    }
                }.launchIn(viewModelScope)

        roomSummaryLive.execute {
            val summary = it.invoke() ?: return@execute this
            if (summary.isEncrypted) {
                copy(
                        isRoomEncrypted = true,
                        isAlgorithmSupported = summary.roomEncryptionAlgorithm is RoomEncryptionAlgorithm.SupportedAlgorithm
                )
            } else {
                copy(isRoomEncrypted = false)
            }
        }
        roomSummaryLive.combine(powerLevelsContentLive) { roomSummary, powerLevelsContent ->
            val roomName = roomSummary.toMatrixItem().getBestName()
            val powerLevelsHelper = PowerLevelsHelper(powerLevelsContent)
            when (val userPowerLevel = powerLevelsHelper.getUserRole(initialState.userId)) {
                Role.Admin -> stringProvider.getString(CommonStrings.room_member_power_level_admin_in, roomName)
                Role.Moderator -> stringProvider.getString(CommonStrings.room_member_power_level_moderator_in, roomName)
                Role.Default -> stringProvider.getString(CommonStrings.room_member_power_level_default_in, roomName)
                is Role.Custom -> stringProvider.getString(CommonStrings.room_member_power_level_custom_in, userPowerLevel.value, roomName)
            }
        }.execute {
            copy(userPowerLevelString = it)
        }
    }

    private fun handleIgnoreAction() = withState { state ->
        val isIgnored = state.isIgnored() ?: return@withState
        _viewEvents.post(RoomMemberProfileViewEvents.Loading())
        viewModelScope.launch {
            val event = try {
                if (isIgnored) {
                    session.userService().unIgnoreUserIds(listOf(state.userId))
                } else {
                    session.userService().ignoreUserIds(listOf(state.userId))
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
